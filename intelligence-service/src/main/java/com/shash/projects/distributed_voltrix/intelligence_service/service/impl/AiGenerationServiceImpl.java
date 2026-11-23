package com.shash.projects.distributed_voltrix.intelligence_service.service.impl;

import com.shash.projects.distributed_voltrix.common_lib.enums.ChatEventStatus;
import com.shash.projects.distributed_voltrix.common_lib.enums.ChatEventType;
import com.shash.projects.distributed_voltrix.common_lib.enums.MessageRole;
import com.shash.projects.distributed_voltrix.common_lib.event.FileStoreRequestEvent;
import com.shash.projects.distributed_voltrix.common_lib.security.AuthUtil;
import com.shash.projects.distributed_voltrix.intelligence_service.client.WorkspaceClient;
import com.shash.projects.distributed_voltrix.intelligence_service.dto.chat.StreamResponse;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatEvent;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatMessage;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatSession;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatSessionId;
import com.shash.projects.distributed_voltrix.intelligence_service.llm.CodeGenerationTools;
import com.shash.projects.distributed_voltrix.intelligence_service.llm.FileTreeContextAdvisor;
import com.shash.projects.distributed_voltrix.intelligence_service.llm.LlmResponseParser;
import com.shash.projects.distributed_voltrix.intelligence_service.llm.PromptUtils;
import com.shash.projects.distributed_voltrix.intelligence_service.repository.ChatEventRepository;
import com.shash.projects.distributed_voltrix.intelligence_service.repository.ChatMessageRepository;
import com.shash.projects.distributed_voltrix.intelligence_service.repository.ChatSessionRepository;
import com.shash.projects.distributed_voltrix.intelligence_service.service.AiGenerationService;
import com.shash.projects.distributed_voltrix.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ChatSessionRepository chatSessionRepository;
    private final LlmResponseParser llmResponseParser;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;
    private final WorkspaceClient workspaceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = createChatSessionIfNotExists(projectId, userId);

        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectId, workspaceClient);

        long startTime = System.currentTimeMillis();
        AtomicLong firstTextTime = new AtomicLong(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        AtomicBoolean finalized = new AtomicBoolean(false);

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                    advisorSpec.params(advisorParams);
                    advisorSpec.advisors(fileTreeContextAdvisor);
                })
                .stream()
                .chatResponse()
                // 1) Extract text + capture usage (usage must be read BEFORE empty chunks are dropped)
                .map(response -> {
                    if (response.getMetadata() != null
                            && response.getMetadata().getUsage() != null
                            && response.getMetadata().getUsage().getTotalTokens() != null
                            && response.getMetadata().getUsage().getTotalTokens() > 0) {
                        usageRef.set(response.getMetadata().getUsage());
                    }

                    if (response.getResult() == null || response.getResult().getOutput() == null) {
                        return "";
                    }
                    String text = response.getResult().getOutput().getText();
                    return text == null ? "" : text;
                })
                // 2) Tool-call deltas arrive with empty text. Do not send them to the client.
                .filter(text -> !text.isEmpty())
                .doOnNext(text -> {
                    firstTextTime.compareAndSet(0L, System.currentTimeMillis());
                    fullResponseBuffer.append(text);
                })
                .doOnError(error -> log.error("Error during streaming for projectId: {}", projectId, error))
                // 3) Runs on complete, error AND client cancel (browser closed / connection dropped).
                //    Placed BEFORE onErrorResume so it can still see ON_ERROR.
                .doFinally(signal -> {
                    if (!finalized.compareAndSet(false, true)) {
                        return;
                    }
                    boolean completed = signal == SignalType.ON_COMPLETE;
                    String fullText = fullResponseBuffer.toString();

                    if (!completed && fullText.isBlank()) {
                        log.warn("Stream ended with {} and no output for projectId: {}; nothing to save",
                                signal, projectId);
                        return;
                    }

                    long end = firstTextTime.get() == 0L ? System.currentTimeMillis() : firstTextTime.get();
                    long duration = (end - startTime) / 1000;
                    Usage usage = usageRef.get();

                    Schedulers.boundedElastic().schedule(() -> {
                        try {
                            finalizeChats(userMessage, chatSession, fullText, duration, usage, userId, completed);
                        } catch (Exception e) {
                            log.error("Failed to finalize chat for projectId: {}", projectId, e);
                        }
                    });
                })
                .onErrorResume(error ->
                        Flux.just("<message>The AI model is busy or failed. Please retry.</message>"))
                .map(StreamResponse::new);
    }

    private void finalizeChats(String userMessage,
                               ChatSession chatSession,
                               String fullText,
                               Long duration,
                               Usage usage,
                               Long userId,
                               boolean completed) {

        Long projectId = chatSession.getId().getProjectId();

        int promptTokens = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
        int completionTokens = usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

        if (usage != null && usage.getTotalTokens() != null) {
            usageService.recordTokenUsage(chatSession.getId().getUserId(), usage.getTotalTokens());
        }

        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(promptTokens)
                        .build()
        );

        ChatMessage assistantChatMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .role(MessageRole.ASSISTANT)
                        .content("Assistant Message here...")
                        .chatSession(chatSession)
                        .tokensUsed(completionTokens)
                        .build()
        );

        List<ChatEvent> chatEventList =
                new ArrayList<>(llmResponseParser.parseChatEvents(fullText, assistantChatMessage));

        // A partial / aborted response must never write files, so drop FILE_EDIT events.
        if (!completed) {
            chatEventList.removeIf(e -> e.getType() == ChatEventType.FILE_EDIT);
        }

        chatEventList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .status(ChatEventStatus.CONFIRMED)
                .chatMessage(assistantChatMessage)
                .content("Thought for " + duration + "s")
                .sequenceOrder(0)
                .build());

        chatEventList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
                .forEach(e -> {
                    String sagaId = UUID.randomUUID().toString();
                    e.setSagaId(sagaId);
                    FileStoreRequestEvent fileStoreRequestEvent = new FileStoreRequestEvent(
                            projectId, sagaId, e.getFilePath(), e.getContent(), userId
                    );
                    log.info("Storage request event sent: {}", e.getFilePath());
                    kafkaTemplate.send("file-storage-request-event", "project-" + projectId, fileStoreRequestEvent);
                });

        chatEventRepository.saveAll(chatEventList);
    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

        if (chatSession == null) {
            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();

            chatSession = chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }
}

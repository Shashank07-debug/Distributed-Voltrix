package com.shash.projects.distributed_voltrix.intelligence_service.llm;

import com.shash.projects.distributed_voltrix.common_lib.dto.FileNode;
import com.shash.projects.distributed_voltrix.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds the project's FILE_TREE and the contents of the main source files to the
 * system prompt, so the model rarely needs to call read_files at all.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    // Tune these to trade prompt size against tool calls.
    private static final int MAX_PRELOAD_FILES = 15;
    private static final int MAX_CHARS_PER_FILE = 12_000;
    private static final int MAX_TOTAL_CHARS = 60_000;

    private static final Set<String> TEXT_EXTENSIONS =
            Set.of(".tsx", ".ts", ".jsx", ".js", ".css", ".html", ".json", ".md");

    private static final List<String> SKIP_MARKERS = List.of(
            "node_modules/", "dist/", "build/", ".git/",
            "package-lock.json", "pnpm-lock.yaml", "yarn.lock"
    );

    private final WorkspaceClient workspaceClient;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {
        Map<String, Object> context = request.context();
        Long projectId = Long.parseLong(context.getOrDefault("projectId", 0).toString());

        ChatClientRequest augmentedChatClientRequest = augmentRequest(request, projectId);

        return streamAdvisorChain.nextStream(augmentedChatClientRequest);
    }

    private ChatClientRequest augmentRequest(ChatClientRequest request, Long projectId) {

        List<Message> incomingMessages = request.prompt().getInstructions();

        // Keep ONE system message: some providers handle several system messages poorly.
        String baseSystemText = incomingMessages.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        List<Message> nonSystemMessages = incomingMessages.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList();

        List<String> paths = workspaceClient.getFileTree(projectId).files().stream()
                .map(FileNode::path) // adjust if your FileNode uses a different accessor
                .filter(Objects::nonNull)
                .toList();

        String projectContext = buildProjectContext(projectId, paths);
        String mergedSystemText = baseSystemText.isBlank()
                ? projectContext
                : baseSystemText + "\n\n" + projectContext;

        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new SystemMessage(mergedSystemText));
        allMessages.addAll(nonSystemMessages);

        return request
                .mutate()
                .prompt(new Prompt(allMessages, request.prompt().getOptions()))
                .build();
    }

    private String buildProjectContext(Long projectId, List<String> paths) {
        StringBuilder sb = new StringBuilder();

        sb.append("---- FILE_TREE ----\n");
        paths.forEach(p -> sb.append(p).append('\n'));

        sb.append("\n---- FILE_CONTENTS ----\n");
        sb.append("The files below are ALREADY LOADED. Do NOT call read_files for them. ")
                .append("For any other file in FILE_TREE you may call read_files once. ")
                .append("After that you MUST stop reading and give your final answer.\n\n");

        List<String> candidates = paths.stream()
                .filter(this::isPreloadable)
                .sorted(Comparator.comparing(p -> !p.startsWith("src/"))) // src/ first, stable otherwise
                .toList();

        int totalChars = 0;
        int loaded = 0;

        for (String path : candidates) {
            if (loaded >= MAX_PRELOAD_FILES || totalChars >= MAX_TOTAL_CHARS) {
                break;
            }

            String cleanPath = path.startsWith("/") ? path.substring(1) : path;

            try {
                String content = workspaceClient.getFileContent(projectId, cleanPath);

                if (content == null || content.isBlank()) {
                    continue;
                }
                // Oversized files stay available through read_files instead of bloating the prompt.
                if (content.length() > MAX_CHARS_PER_FILE
                        || totalChars + content.length() > MAX_TOTAL_CHARS) {
                    continue;
                }

                sb.append(String.format(
                        "--- START OF FILE: %s ---\n%s\n--- END OF FILE ---\n\n",
                        cleanPath, content
                ));
                totalChars += content.length();
                loaded++;
            } catch (Exception e) {
                log.warn("Could not preload '{}' for projectId {}: {}", cleanPath, projectId, e.getMessage());
            }
        }

        log.info("Preloaded {} files ({} chars) for projectId {}", loaded, totalChars, projectId);
        return sb.toString();
    }

    private boolean isPreloadable(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (SKIP_MARKERS.stream().anyMatch(p::contains)) {
            return false;
        }
        return TEXT_EXTENSIONS.stream().anyMatch(p::endsWith);
    }

    @Override
    public String getName() {
        return "FileTreeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

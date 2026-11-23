package com.shash.projects.distributed_voltrix.intelligence_service.llm;

import com.shash.projects.distributed_voltrix.common_lib.enums.ChatEventStatus;
import com.shash.projects.distributed_voltrix.common_lib.enums.ChatEventType;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatEvent;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class LlmResponseParser {

    private static final Pattern GENERIC_TAG_PATTERN = Pattern.compile(
            "(<(message|file|tool)(\\s[^>]*)?>)([\\s\\S]*?)(</\\2\\s*>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(path|args)\\s*=\\s*[\"']([^\"']+)[\"']"
    );

    public List<ChatEvent> parseChatEvents(String fullResponse, ChatMessage parentMessage) {
        List<ChatEvent> events = new ArrayList<>();
        int orderCounter = 1;

        if (fullResponse == null || fullResponse.isBlank()) {
            return events;
        }

        Matcher matcher = GENERIC_TAG_PATTERN.matcher(fullResponse);

        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            String content = cleanContent(tagName, matcher.group(4));

            Map<String, String> attrMap = extractAttributes(attributes);

            ChatEvent.ChatEventBuilder builder = ChatEvent.builder()
                    .status(ChatEventStatus.CONFIRMED)
                    .chatMessage(parentMessage)
                    .content(content)
                    .sequenceOrder(orderCounter++);

            switch (tagName) {
                case "message" -> builder.type(ChatEventType.MESSAGE);
                case "file" -> {
                    builder.type(ChatEventType.FILE_EDIT);
                    builder.status(ChatEventStatus.PENDING);
                    builder.filePath(attrMap.get("path"));
                }
                case "tool" -> {
                    builder.type(ChatEventType.TOOL_LOG);
                    builder.metadata(attrMap.get("args"));
                }
                default -> { continue; }
            }

            events.add(builder.build());
        }

        return events;
    }

    private String cleanContent(String tagName, String raw) {
        String text = raw == null ? "" : raw.trim();
        if (tagName.equals("file")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*\\n", "");
            text = text.replaceFirst("\\n?```\\s*$", "");
        }
        return text.trim();
    }

    private Map<String, String> extractAttributes(String attributeString) {
        Map<String, String> attributes = new HashMap<>();
        if (attributeString == null) return attributes;

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributeString);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(2));
        }
        return attributes;
    }

}
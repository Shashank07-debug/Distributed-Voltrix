package com.shash.projects.distributed_voltrix.intelligence_service.dto.chat;


import com.shash.projects.distributed_voltrix.common_lib.enums.MessageRole;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        Long id,
        MessageRole role,
        List<ChatEventResponse> events,
        String content,
        Integer tokenUsed,
        Instant createdAt

) {
}

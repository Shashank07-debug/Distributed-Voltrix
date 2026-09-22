package com.shash.projects.distributed_voltrix.intelligence_service.dto.chat;


import com.shash.projects.distributed_voltrix.common_lib.enums.ChatEventType;

public record ChatEventResponse(
        Long id,
        ChatEventType type,
        Integer sequenceOrder,
        String content,
        String filePath,
        String metadata
) {
}

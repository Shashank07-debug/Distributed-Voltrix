package com.shash.projects.distributed_voltrix.workspace_service.dto.project;


import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}

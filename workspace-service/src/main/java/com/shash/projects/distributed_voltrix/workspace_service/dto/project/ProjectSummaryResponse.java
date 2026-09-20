package com.shash.projects.distributed_voltrix.workspace_service.dto.project;



import com.shash.projects.distributed_voltrix.common_lib.enums.ProjectRole;

import java.time.Instant;

public record ProjectSummaryResponse (
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        ProjectRole role
){
}

package com.shash.projects.distributed_voltrix.workspace_service.dto.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank String name
        ) {
}

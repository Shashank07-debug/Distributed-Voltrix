package com.shash.projects.distributed_voltrix.workspace_service.dto.member;


import com.shash.projects.distributed_voltrix.common_lib.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull ProjectRole role
) {
}

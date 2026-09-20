package com.shash.projects.distributed_voltrix.workspace_service.dto.member;

import com.shash.projects.distributed_voltrix.common_lib.enums.ProjectRole;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String username,
        String name,
        ProjectRole projectRole,
        Instant invitedAt
) {

}

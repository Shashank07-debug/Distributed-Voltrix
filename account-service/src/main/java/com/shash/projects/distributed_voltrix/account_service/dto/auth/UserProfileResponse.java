package com.shash.projects.distributed_voltrix.account_service.dto.auth;

public record UserProfileResponse(
        Long id,
        String username,
        String name
) {
}

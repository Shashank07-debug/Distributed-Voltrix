package com.shash.projects.distributed_voltrix.account_service.dto.auth;

public record AuthResponse(
        String token,
        UserProfileResponse user
) {


}

package com.shash.projects.distributed_voltrix.account_service.dto.subscription;

public record UsageTodayResponse(
        Integer tokenUsed,
        Integer tokensLimit,
        Integer previewRunning,
        Integer previewsList
) {
}

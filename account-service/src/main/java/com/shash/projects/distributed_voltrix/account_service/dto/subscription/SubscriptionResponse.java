package com.shash.projects.distributed_voltrix.account_service.dto.subscription;

import com.shash.projects.distributed_voltrix.common_lib.dto.PlanDto;

import java.time.Instant;

public record SubscriptionResponse(
        PlanDto plan,
        String status,
        Instant periodEnd,
        Long tokenUsedThisCycle
) {
}

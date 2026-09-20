package com.shash.projects.distributed_voltrix.account_service.repository;


import com.shash.projects.distributed_voltrix.account_service.entity.Subscription;
import com.shash.projects.distributed_voltrix.common_lib.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByGatewaySubscriptionId(String subscriptionId);

    Optional<Subscription> findByGatewaySubscriptionId(String gatewaySubscriptionId);
}

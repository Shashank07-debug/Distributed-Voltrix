package com.shash.projects.distributed_voltrix.account_service.mapper;


import com.shash.projects.distributed_voltrix.account_service.dto.subscription.PlanResponse;
import com.shash.projects.distributed_voltrix.account_service.dto.subscription.SubscriptionResponse;
import com.shash.projects.distributed_voltrix.account_service.entity.Plan;
import com.shash.projects.distributed_voltrix.account_service.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanResponse toPlanResponse(Plan plan);
}

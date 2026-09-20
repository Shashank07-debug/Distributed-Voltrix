package com.shash.projects.distributed_voltrix.account_service.mapper;


import com.shash.projects.distributed_voltrix.account_service.dto.subscription.SubscriptionResponse;
import com.shash.projects.distributed_voltrix.account_service.entity.Plan;
import com.shash.projects.distributed_voltrix.account_service.entity.Subscription;
import com.shash.projects.distributed_voltrix.common_lib.dto.PlanDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);
}

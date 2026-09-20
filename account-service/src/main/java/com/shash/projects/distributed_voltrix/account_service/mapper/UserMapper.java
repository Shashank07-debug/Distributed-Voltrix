package com.shash.projects.distributed_voltrix.account_service.mapper;


import com.shash.projects.distributed_voltrix.account_service.dto.auth.SignupRequest;
import com.shash.projects.distributed_voltrix.account_service.dto.auth.UserProfileResponse;
import com.shash.projects.distributed_voltrix.account_service.entity.User;
import com.shash.projects.distributed_voltrix.common_lib.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignupRequest signupRequest);
    UserProfileResponse toUserProfileResponse(User user);
    UserDto toUserDto(User user);
}

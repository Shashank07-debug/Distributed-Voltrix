package com.shash.projects.distributed_voltrix.account_service.mapper;


import com.shash.projects.distributed_voltrix.account_service.dto.auth.SignupRequest;
import com.shash.projects.distributed_voltrix.account_service.dto.auth.UserProfileResponse;
import com.shash.projects.distributed_voltrix.account_service.entity.User;
import com.shash.projects.distributed_voltrix.common_lib.dto.UserDto;
import com.shash.projects.distributed_voltrix.common_lib.security.JwtUserPrinciple;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignupRequest signupRequest);

    @Mapping(source = "userId", target = "id")
    UserProfileResponse toUserProfileResponse(JwtUserPrinciple user);

    UserDto toUserDto(User user);
}

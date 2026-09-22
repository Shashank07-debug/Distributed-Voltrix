package com.shash.projects.distributed_voltrix.common_lib.security;

import jakarta.annotation.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record JwtUserPrinciple (
        Long userId,
        String name,
        String username,
        String password,
        List<GrantedAuthority> authorities
) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername(){
        return username;
    }
}

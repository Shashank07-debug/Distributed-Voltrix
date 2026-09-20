package com.shash.projects.distributed_voltrix.account_service.service;


import com.shash.projects.distributed_voltrix.account_service.dto.auth.AuthResponse;
import com.shash.projects.distributed_voltrix.account_service.dto.auth.LoginRequest;
import com.shash.projects.distributed_voltrix.account_service.dto.auth.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);
}

package com.stockbridge.accounts.service;

import com.stockbridge.accounts.dto.AuthResponse;
import com.stockbridge.accounts.dto.LoginRequest;
import com.stockbridge.accounts.dto.RegisterRequest;
import com.stockbridge.accounts.dto.UserProfileResponse;

public interface AuthService {
    UserProfileResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getCurrentUser(String email);
}

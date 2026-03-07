package com.evalx.service;

import com.evalx.dto.LoginRequest;
import com.evalx.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.auth.admin-user}")
    private String adminUser;

    @Value("${app.auth.admin-password}")
    private String adminPassword;

    @Value("${app.auth.admin-token}")
    private String adminToken;

    public LoginResponse login(LoginRequest request) {
        if (adminUser.equals(request.getUsername()) && adminPassword.equals(request.getPassword())) {
            return LoginResponse.builder()
                    .token(adminToken)
                    .username(adminUser)
                    .build();
        }
        throw new RuntimeException("Invalid credentials");
    }
}

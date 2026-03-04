package com.evalx.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AuthConfig {

    @Value("${app.auth.admin-user:admin}")
    private String adminUser;

    @Value("${app.auth.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.auth.admin-token:evalx-admin-secret-token-2026}")
    private String adminToken;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;
}

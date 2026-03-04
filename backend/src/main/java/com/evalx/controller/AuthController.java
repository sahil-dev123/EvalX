package com.evalx.controller;

import com.evalx.dto.request.LoginRequest;
import com.evalx.dto.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Hardcoded for MVP, in production this should be in DB + proper JWT
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    public static final String ADMIN_TOKEN = "evalx-admin-secret-token-2026";

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        if (ADMIN_USER.equals(request.getUsername()) && ADMIN_PASS.equals(request.getPassword())) {
            return ResponseEntity.ok(ApiResponse.ok("Login successful", Map.of("token", ADMIN_TOKEN)));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }
}

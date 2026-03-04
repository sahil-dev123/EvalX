package com.evalx.controller;

import com.evalx.config.AuthConfig;
import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.LoginRequest;
import com.evalx.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiConstants.AUTH_API)
@RequiredArgsConstructor
public class AuthController {

    private final AuthConfig authConfig;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        log.info(LogConstants.START_METHOD, "login");
        if (authConfig.getAdminUser().equals(request.getUsername())
                && authConfig.getAdminPassword().equals(request.getPassword())) {
            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.ok("Login successful", Map.of("token", authConfig.getAdminToken())));
        }
        log.warn("Login failed for user: {}", request.getUsername());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }
}

package com.evalx.controller;

import com.evalx.dto.ApiResponse;
import com.evalx.dto.LoginRequest;
import com.evalx.dto.LoginResponse;
import com.evalx.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}

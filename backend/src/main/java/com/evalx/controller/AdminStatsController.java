package com.evalx.controller;

import com.evalx.dto.AdminStatsResponse;
import com.evalx.dto.ApiResponse;
import com.evalx.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/stats")
    public ApiResponse<AdminStatsResponse> getStats() {
        return ApiResponse.success(adminStatsService.getStats());
    }
}

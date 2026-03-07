package com.evalx.controller;

import com.evalx.dto.ApiResponse;
import com.evalx.entity.ExamYear;
import com.evalx.repository.ExamYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/exam-years")
@RequiredArgsConstructor
public class ExamYearController {

    private final ExamYearRepository examYearRepository;

    @GetMapping("/by-stage/{stageId}")
    public ApiResponse<List<ExamYear>> getYearsByStage(@PathVariable Long stageId) {
        log.info("GET_YEARS: Fetching years for Stage ID: {}", stageId);
        List<ExamYear> years = examYearRepository.findByStageId(stageId);
        log.info("YEARS_RESPONSE: Found {} years for Stage ID: {}", years.size(), stageId);
        return ApiResponse.success(years);
    }
}

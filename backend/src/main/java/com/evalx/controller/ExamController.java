package com.evalx.controller;

import com.evalx.dto.ApiResponse;
import com.evalx.entity.Exam;
import com.evalx.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;

    @GetMapping
    public ApiResponse<List<Exam>> getAllExams() {
        log.info("GET /api/exams");
        List<Exam> exams = examService.getAllExams();
        log.info("Found {} exams in DB", exams.size());
        return ApiResponse.success(exams);
    }

    @PostMapping("/seed")
    public ApiResponse<String> seedData() {
        examService.seedInitialData();
        return ApiResponse.success("Data seeded successfully");
    }
}

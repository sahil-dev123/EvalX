package com.evalx.controller;

import com.evalx.dto.ApiResponse;
import com.evalx.entity.ExamStage;
import com.evalx.repository.ExamStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/exam-stages")
@RequiredArgsConstructor
public class ExamStageController {

    private final ExamStageRepository examStageRepository;

    @GetMapping("/by-exam/{examId}")
    public ApiResponse<List<ExamStage>> getStagesByExam(@PathVariable Long examId) {
        log.info("GET_STAGES: Fetching stages for Exam ID: {}", examId);
        List<ExamStage> stages = examStageRepository.findByExamId(examId);
        log.info("STAGES_RESPONSE: Found {} stages for Exam ID: {}", stages.size(), examId);
        return ApiResponse.success(stages);
    }
}

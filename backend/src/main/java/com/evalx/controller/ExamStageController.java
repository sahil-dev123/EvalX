package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateExamStageRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.ExamStageResponse;
import com.evalx.service.ExamManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiConstants.EXAM_STAGES_API)
@RequiredArgsConstructor
public class ExamStageController {

    private final ExamManagementService examManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamStageResponse>> createStage(@Valid @RequestBody CreateExamStageRequest req) {
        log.info(LogConstants.START_METHOD, "createStage");
        ExamStageResponse response = examManagementService.createStage(req);
        log.info(LogConstants.END_METHOD, "createStage");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stage created", response));
    }

    @GetMapping("/by-exam/{examId}")
    public ResponseEntity<ApiResponse<List<ExamStageResponse>>> getStagesByExam(@PathVariable Long examId) {
        log.info(LogConstants.START_PROCESS, "getStagesByExam", examId);
        List<ExamStageResponse> stages = examManagementService.getStagesByExamId(examId);
        log.info(LogConstants.DATA_LOADED, stages.size(), "ExamStage");
        return ResponseEntity.ok(ApiResponse.ok(stages));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "deleteStage", id);
        examManagementService.deleteStage(id);
        log.info(LogConstants.COMPLETED_PROCESS, "deleteStage", id);
        return ResponseEntity.ok(ApiResponse.ok("Stage deleted", null));
    }
}

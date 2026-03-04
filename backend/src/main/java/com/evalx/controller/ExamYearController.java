package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateExamYearRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.ExamYearResponse;
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
@RequestMapping(ApiConstants.EXAM_YEARS_API)
@RequiredArgsConstructor
public class ExamYearController {

    private final ExamManagementService examManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamYearResponse>> createExamYear(@Valid @RequestBody CreateExamYearRequest req) {
        log.info(LogConstants.START_METHOD, "createExamYear");
        ExamYearResponse response = examManagementService.createExamYear(req);
        log.info(LogConstants.END_METHOD, "createExamYear");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Exam year created", response));
    }

    @GetMapping("/by-stage/{stageId}")
    public ResponseEntity<ApiResponse<List<ExamYearResponse>>> getYearsByStage(@PathVariable Long stageId) {
        log.info(LogConstants.START_PROCESS, "getYearsByStage", stageId);
        List<ExamYearResponse> years = examManagementService.getYearsByStageId(stageId);
        log.info(LogConstants.DATA_LOADED, years.size(), "ExamYear");
        return ResponseEntity.ok(ApiResponse.ok(years));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamYearResponse>> getExamYear(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "getExamYear", id);
        // Directly convert the entity to response — avoids redundant list-and-filter
        ExamYearResponse response = examManagementService.getExamYearById(id);
        log.info(LogConstants.COMPLETED_PROCESS, "getExamYear", id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/candidates")
    public ResponseEntity<ApiResponse<ExamYearResponse>> updateCandidates(@PathVariable Long id,
            @RequestParam Long totalCandidates) {
        log.info("Updating totalCandidates for examYearId={} to {}", id, totalCandidates);
        ExamYearResponse response = examManagementService.updateTotalCandidates(id, totalCandidates);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExamYear(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "deleteExamYear", id);
        examManagementService.deleteExamYear(id);
        log.info(LogConstants.COMPLETED_PROCESS, "deleteExamYear", id);
        return ResponseEntity.ok(ApiResponse.ok("Exam year deleted", null));
    }
}

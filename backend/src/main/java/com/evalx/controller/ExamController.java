package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateExamRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.ExamResponse;
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
@RequestMapping(ApiConstants.EXAMS_API)
@RequiredArgsConstructor
public class ExamController {

    private final ExamManagementService examManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(@Valid @RequestBody CreateExamRequest request) {
        log.info(LogConstants.START_METHOD, "createExam");
        ExamResponse response = examManagementService.createExam(request);
        log.info("Created exam: id={}, code={}", response.getId(), response.getCode());
        log.info(LogConstants.END_METHOD, "createExam");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Exam created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getAllExams() {
        log.info(LogConstants.START_METHOD, "getAllExams");
        List<ExamResponse> exams = examManagementService.getAllExams();
        log.info(LogConstants.DATA_LOADED, exams.size(), "Exam");
        return ResponseEntity.ok(ApiResponse.ok(exams));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamResponse>> getExamById(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "getExamById", id);
        ExamResponse exam = examManagementService.getExamById(id);
        log.info(LogConstants.COMPLETED_PROCESS, "getExamById", id);
        return ResponseEntity.ok(ApiResponse.ok(exam));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "deleteExam", id);
        examManagementService.deleteExam(id);
        log.info(LogConstants.COMPLETED_PROCESS, "deleteExam", id);
        return ResponseEntity.ok(ApiResponse.ok("Exam deleted", null));
    }
}

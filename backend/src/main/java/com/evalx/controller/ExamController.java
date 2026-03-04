package com.evalx.controller;

import com.evalx.dto.request.CreateExamRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.ExamResponse;
import com.evalx.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(@Valid @RequestBody CreateExamRequest request) {
        ExamResponse response = examService.createExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Exam created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getAllExams() {
        return ResponseEntity.ok(ApiResponse.ok(examService.getAllExams()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamResponse>> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(examService.getExamById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.ok(ApiResponse.ok("Exam deleted", null));
    }
}

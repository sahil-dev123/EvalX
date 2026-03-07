package com.evalx.controller;

import com.evalx.dto.ApiResponse;
import com.evalx.service.AdminUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUploadService adminUploadService;

    @PostMapping("/ingest")
    public ApiResponse<String> ingest(
            @RequestParam(required = false) MultipartFile questionPaper,
            @RequestParam(required = false) String questionPaperUrl,
            @RequestParam(required = false) MultipartFile answerKey,
            @RequestParam(required = false) String answerKeyUrl,
            @RequestParam(required = false) String examCode,
            @RequestParam(required = false) String stageName,
            @RequestParam(required = false) Integer year) throws IOException {

        log.info("ADMIN_INGEST_REQUEST: Metadata - Exam: {}, Stage: {}, Year: {}", examCode, stageName, year);

        String result = adminUploadService.ingest(
                questionPaper, questionPaperUrl,
                answerKey, answerKeyUrl,
                examCode, stageName, year);

        return ApiResponse.success(result);
    }

    @PostMapping("/upload-question-paper")
    public ApiResponse<String> uploadQuestionPaper(
            @RequestParam Long examId,
            @RequestParam MultipartFile file) throws IOException {
        adminUploadService.uploadQuestionPaper(examId, file);
        return ApiResponse.success("Question paper uploaded successfully (legacy)");
    }
}

package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.response.ApiResponse;
import com.evalx.service.QuestionPaperParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping(ApiConstants.ADMIN_API)
@RequiredArgsConstructor
public class AdminController {

    private final QuestionPaperParserService questionPaperParserService;

    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<String>> ingestMasterPdfs(
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("answerKey") MultipartFile answerKey) throws IOException {

        log.info(LogConstants.START_INGEST, questionPaper.getOriginalFilename(), answerKey.getOriginalFilename());
        questionPaperParserService.universalIngest(questionPaper, answerKey);
        log.info(LogConstants.COMPLETED_INGEST);

        return ResponseEntity.ok(ApiResponse.ok("Shift successfully seeded with Magic Ingest!", null));
    }
}

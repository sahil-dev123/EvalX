package com.evalx.controller;

import com.evalx.dto.ApiResponse;
import com.evalx.dto.ResultResponse;
import com.evalx.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/debug/pdf")
    public String debugPdf(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("DEBUG_PDF: Extracting text from {}", file.getOriginalFilename());
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("PDF_TEXT_START\n{}\nPDF_TEXT_END", text);
            return "Check logs for PDF text. Length: " + text.length();
        } catch (Exception e) {
            log.error("DEBUG_PDF_ERROR: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/evaluate")
    public ApiResponse<ResultResponse> evaluate(
            @RequestParam(required = false) Long examYearId,
            @RequestParam(required = false) Long examId,
            @RequestParam MultipartFile file) throws IOException {
        log.info("EVAL_REQUEST: Received evaluation request for ExamYear ID: {}", examYearId);
        log.info("FILE_INFO: Received file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        ResultResponse response = evaluationService.evaluate(examYearId, examId, file);

        log.info("EVAL_RESPONSE_SENT: Success. Score: {}", response.getTotalScore());
        return ApiResponse.success(response);
    }
}

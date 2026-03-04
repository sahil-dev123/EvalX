package com.evalx.controller;

import com.evalx.dto.request.BulkQuestionRequest;
import com.evalx.dto.request.CreateQuestionRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.Question;
import com.evalx.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createQuestion(@Valid @RequestBody CreateQuestionRequest req) {
        Question q = questionService.createQuestion(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Question created",
                Map.of("id", q.getId(), "questionNumber", q.getQuestionNumber(), "type", q.getQuestionType())));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkCreate(@Valid @RequestBody BulkQuestionRequest req) {
        List<Question> questions = questionService.bulkCreateQuestions(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Questions created",
                Map.of("count", questions.size())));
    }

    @GetMapping("/by-exam-year/{examYearId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuestions(@PathVariable Long examYearId) {
        List<Map<String, Object>> questions = questionService.getQuestionsByExamYearId(examYearId).stream()
                .map(q -> Map.<String, Object>of(
                        "id", q.getId(),
                        "questionNumber", q.getQuestionNumber(),
                        "type", q.getQuestionType().name(),
                        "sectionName", q.getSection().getName(),
                        "correctAnswer", q.getAnswerKey() != null ? q.getAnswerKey().getCorrectAnswer() : "N/A"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(questions));
    }
}

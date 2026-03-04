package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.BulkQuestionRequest;
import com.evalx.dto.request.CreateQuestionRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.Question;
import com.evalx.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(ApiConstants.QUESTIONS_API)
@RequiredArgsConstructor
public class QuestionController {

        private final QuestionService questionService;

        @PostMapping
        public ResponseEntity<ApiResponse<Map<String, Object>>> createQuestion(
                        @Valid @RequestBody CreateQuestionRequest req) {
                log.info(LogConstants.START_METHOD, "createQuestion");
                Question q = questionService.createQuestion(req);
                log.info("Created question: id={}, questionNumber={}, type={}", q.getId(), q.getQuestionNumber(),
                                q.getQuestionType());
                log.info(LogConstants.END_METHOD, "createQuestion");
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Question created",
                                Map.of("id", q.getId(), "questionNumber", q.getQuestionNumber(), "type",
                                                q.getQuestionType())));
        }

        @PostMapping("/bulk")
        public ResponseEntity<ApiResponse<Map<String, Object>>> bulkCreate(
                        @Valid @RequestBody BulkQuestionRequest req) {
                log.info(LogConstants.START_METHOD, "bulkCreate");
                log.debug("Bulk creating questions for sectionId={}, count={}", req.getSectionId(),
                                req.getQuestions().size());
                List<Question> questions = questionService.bulkCreateQuestions(req);
                log.info("Bulk created {} questions for sectionId={}", questions.size(), req.getSectionId());
                log.info(LogConstants.END_METHOD, "bulkCreate");
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Questions created",
                                Map.of("count", questions.size())));
        }

        @GetMapping("/by-exam-year/{examYearId}")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuestions(@PathVariable Long examYearId) {
                log.info(LogConstants.START_PROCESS, "getQuestions", examYearId);
                List<Map<String, Object>> questions = questionService.getQuestionsByExamYearId(examYearId).stream()
                                .map(q -> Map.<String, Object>of(
                                                "id", q.getId(),
                                                "questionNumber", q.getQuestionNumber(),
                                                "type", q.getQuestionType().name(),
                                                "sectionName", q.getSection().getName(),
                                                "correctAnswer",
                                                q.getAnswerKey() != null ? q.getAnswerKey().getCorrectAnswer() : "N/A"))
                                .collect(Collectors.toList());
                log.info(LogConstants.DATA_LOADED, questions.size(), "Question");
                return ResponseEntity.ok(ApiResponse.ok(questions));
        }
}

package com.evalx.service;

import com.evalx.constants.LogConstants;
import com.evalx.dto.request.BulkQuestionRequest;
import com.evalx.dto.request.CreateQuestionRequest;
import com.evalx.entity.*;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.AnswerKeyRepository;
import com.evalx.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerKeyRepository answerKeyRepository;
    private final SectionService sectionService;

    @Transactional
    public Question createQuestion(CreateQuestionRequest request) {
        log.info(LogConstants.START_METHOD, "createQuestion");
        Section section = sectionService.findSectionById(request.getSectionId());
        Question question = Question.builder()
                .section(section)
                .questionNumber(request.getQuestionNumber())
                .questionType(request.getQuestionType() != null ? request.getQuestionType() : QuestionType.MCQ)
                .build();
        question = questionRepository.save(question);

        if (request.getCorrectAnswer() != null) {
            AnswerKey ak = AnswerKey.builder()
                    .question(question)
                    .correctAnswer(request.getCorrectAnswer().toUpperCase().trim())
                    .build();
            answerKeyRepository.save(ak);
            question.setAnswerKey(ak);
        }
        log.info(LogConstants.END_METHOD, "createQuestion");
        return question;
    }

    @Transactional
    public List<Question> bulkCreateQuestions(BulkQuestionRequest request) {
        log.info(LogConstants.START_METHOD, "bulkCreateQuestions");
        Section section = sectionService.findSectionById(request.getSectionId());
        List<Question> created = new ArrayList<>();

        for (BulkQuestionRequest.QuestionItem item : request.getQuestions()) {
            QuestionType type = QuestionType.MCQ;
            if (item.getQuestionType() != null) {
                try {
                    type = QuestionType.valueOf(item.getQuestionType().toUpperCase());
                } catch (Exception ignored) {
                }
            }

            Question question = Question.builder()
                    .section(section)
                    .questionNumber(item.getQuestionNumber())
                    .questionText(item.getQuestionText())
                    .questionHash(item.getQuestionHash())
                    .questionType(type)
                    .build();
            question = questionRepository.save(question);

            if (item.getCorrectAnswer() != null) {
                AnswerKey ak = AnswerKey.builder()
                        .question(question)
                        .correctAnswer(item.getCorrectAnswer().toUpperCase().trim())
                        .build();
                answerKeyRepository.save(ak);
                question.setAnswerKey(ak);
            }
            created.add(question);
        }
        log.info("Bulk created {} questions", created.size());
        log.info(LogConstants.END_METHOD, "bulkCreateQuestions");
        return created;
    }

    public List<Question> getQuestionsByExamYearId(Long examYearId) {
        log.info(LogConstants.START_PROCESS, "getQuestionsByExamYearId", examYearId);
        List<Question> questions = questionRepository.findWithAnswerKeyByExamYearId(examYearId);
        log.info(LogConstants.DATA_LOADED, questions.size(), "Question");
        return questions;
    }

    public Question findQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
    }
}

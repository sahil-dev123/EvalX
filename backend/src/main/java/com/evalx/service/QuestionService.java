package com.evalx.service;

import com.evalx.dto.request.BulkQuestionRequest;
import com.evalx.dto.request.CreateQuestionRequest;
import com.evalx.entity.*;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.AnswerKeyRepository;
import com.evalx.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerKeyRepository answerKeyRepository;
    private final SectionService sectionService;

    @Transactional
    public Question createQuestion(CreateQuestionRequest request) {
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
        return question;
    }

    @Transactional
    public List<Question> bulkCreateQuestions(BulkQuestionRequest request) {
        Section section = sectionService.findSectionById(request.getSectionId());
        List<Question> created = new ArrayList<>();

        for (BulkQuestionRequest.QuestionItem item : request.getQuestions()) {
            QuestionType type = QuestionType.MCQ;
            if (item.getQuestionType() != null) {
                try { type = QuestionType.valueOf(item.getQuestionType().toUpperCase()); }
                catch (Exception ignored) {}
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
        return created;
    }

    public List<Question> getQuestionsByExamYearId(Long examYearId) {
        return questionRepository.findWithAnswerKeyByExamYearId(examYearId);
    }

    public Question findQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
    }
}

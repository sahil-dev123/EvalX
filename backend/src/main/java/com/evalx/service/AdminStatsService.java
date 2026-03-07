package com.evalx.service;

import com.evalx.dto.AdminStatsResponse;
import com.evalx.repository.ExamRepository;
import com.evalx.repository.ExamStageRepository;
import com.evalx.repository.QuestionRepository;
import com.evalx.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ExamRepository examRepository;
    private final ExamStageRepository examStageRepository;
    private final QuestionRepository questionRepository;
    private final ResultRepository resultRepository;

    public AdminStatsResponse getStats() {
        log.info("AdminStatsService.getStats called to aggregate dashboard metrics");

        long examCount = examRepository.count();
        long stageCount = examStageRepository.count();
        long questionCount = questionRepository.count();
        long evaluationCount = resultRepository.count();

        log.info("Stats aggregated: Exams={}, Stages={}, Questions={}, Evaluations={}",
                examCount, stageCount, questionCount, evaluationCount);

        return AdminStatsResponse.builder()
                .totalExams(examCount)
                .totalStages(stageCount)
                .totalQuestions(questionCount)
                .totalEvaluations(evaluationCount)
                .build();
    }
}

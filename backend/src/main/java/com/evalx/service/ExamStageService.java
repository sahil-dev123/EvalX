package com.evalx.service;

import com.evalx.dto.request.CreateExamStageRequest;
import com.evalx.dto.response.ExamStageResponse;
import com.evalx.entity.Exam;
import com.evalx.entity.ExamStage;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.ExamStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamStageService {

    private final ExamStageRepository examStageRepository;
    private final ExamService examService;

    @Transactional
    public ExamStageResponse createStage(CreateExamStageRequest request) {
        Exam exam = examService.findExamById(request.getExamId());
        ExamStage stage = ExamStage.builder()
                .exam(exam)
                .name(request.getName())
                .description(request.getDescription())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();
        return toResponse(examStageRepository.save(stage));
    }

    public List<ExamStageResponse> getStagesByExamId(Long examId) {
        return examStageRepository.findByExamIdOrderByOrderIndexAsc(examId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExamStage findStageById(Long id) {
        return examStageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam stage not found with id: " + id));
    }

    @Transactional
    public void deleteStage(Long id) {
        ExamStage stage = findStageById(id);
        examStageRepository.delete(stage);
    }

    private ExamStageResponse toResponse(ExamStage stage) {
        List<ExamStageResponse.YearInfo> years = stage.getExamYears() != null
                ? stage.getExamYears().stream()
                .map(y -> ExamStageResponse.YearInfo.builder()
                        .id(y.getId())
                        .year(y.getYear())
                        .totalCandidates(y.getTotalCandidates())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return ExamStageResponse.builder()
                .id(stage.getId())
                .examId(stage.getExam().getId())
                .examName(stage.getExam().getName())
                .name(stage.getName())
                .description(stage.getDescription())
                .orderIndex(stage.getOrderIndex())
                .years(years)
                .build();
    }
}

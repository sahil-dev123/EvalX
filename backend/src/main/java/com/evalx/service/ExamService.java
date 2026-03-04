package com.evalx.service;

import com.evalx.dto.request.CreateExamRequest;
import com.evalx.dto.response.ExamResponse;
import com.evalx.entity.Exam;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;

    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        Exam exam = Exam.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .build();
        return toResponse(examRepository.save(exam));
    }

    public List<ExamResponse> getAllExams() {
        return examRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExamResponse getExamById(Long id) {
        return toResponse(findExamById(id));
    }

    public Exam findExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = findExamById(id);
        examRepository.delete(exam);
    }

    private ExamResponse toResponse(Exam exam) {
        List<ExamResponse.StageInfo> stages = exam.getStages() != null
                ? exam.getStages().stream()
                .map(s -> ExamResponse.StageInfo.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .orderIndex(s.getOrderIndex())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return ExamResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .code(exam.getCode())
                .description(exam.getDescription())
                .iconUrl(exam.getIconUrl())
                .stages(stages)
                .build();
    }
}

package com.evalx.service;

import com.evalx.dto.request.CreateExamYearRequest;
import com.evalx.dto.response.ExamYearResponse;
import com.evalx.entity.ExamStage;
import com.evalx.entity.ExamYear;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.ExamYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamYearService {

    private final ExamYearRepository examYearRepository;
    private final ExamStageService examStageService;

    @Transactional
    public ExamYearResponse createExamYear(CreateExamYearRequest request) {
        ExamStage stage = examStageService.findStageById(request.getExamStageId());
        ExamYear examYear = ExamYear.builder()
                .examStage(stage)
                .year(request.getYear())
                .totalCandidates(request.getTotalCandidates() != null ? request.getTotalCandidates() : 0L)
                .totalMarks(request.getTotalMarks())
                .timeMinutes(request.getTimeMinutes())
                .build();
        return toResponse(examYearRepository.save(examYear));
    }

    public List<ExamYearResponse> getYearsByStageId(Long stageId) {
        return examYearRepository.findByExamStageIdOrderByYearDesc(stageId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExamYear findExamYearById(Long id) {
        return examYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam year not found with id: " + id));
    }

    @Transactional
    public ExamYearResponse updateTotalCandidates(Long id, Long totalCandidates) {
        ExamYear ey = findExamYearById(id);
        ey.setTotalCandidates(totalCandidates);
        return toResponse(examYearRepository.save(ey));
    }

    @Transactional
    public void deleteExamYear(Long id) {
        ExamYear ey = findExamYearById(id);
        examYearRepository.delete(ey);
    }

    private ExamYearResponse toResponse(ExamYear ey) {
        List<ExamYearResponse.ShiftInfo> shifts = ey.getShifts() != null
                ? ey.getShifts().stream()
                .map(s -> ExamYearResponse.ShiftInfo.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .shiftDate(s.getShiftDate())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return ExamYearResponse.builder()
                .id(ey.getId())
                .examStageId(ey.getExamStage().getId())
                .stageName(ey.getExamStage().getName())
                .examName(ey.getExamStage().getExam().getName())
                .year(ey.getYear())
                .totalCandidates(ey.getTotalCandidates())
                .totalMarks(ey.getTotalMarks())
                .timeMinutes(ey.getTimeMinutes())
                .shifts(shifts)
                .build();
    }
}

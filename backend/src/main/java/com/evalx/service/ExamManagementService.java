package com.evalx.service;

import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateExamRequest;
import com.evalx.dto.request.CreateExamStageRequest;
import com.evalx.dto.request.CreateExamYearRequest;
import com.evalx.dto.request.CreateShiftRequest;
import com.evalx.dto.response.ExamResponse;
import com.evalx.dto.response.ExamStageResponse;
import com.evalx.dto.response.ExamYearResponse;
import com.evalx.entity.Exam;
import com.evalx.entity.ExamStage;
import com.evalx.entity.ExamYear;
import com.evalx.entity.Shift;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.ExamRepository;
import com.evalx.repository.ExamStageRepository;
import com.evalx.repository.ExamYearRepository;
import com.evalx.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamManagementService {

        private final ExamRepository examRepository;
        private final ExamStageRepository examStageRepository;
        private final ExamYearRepository examYearRepository;
        private final ShiftRepository shiftRepository;

        // --- Exam Logic ---

        @Transactional
        public ExamResponse createExam(CreateExamRequest request) {
                log.info(LogConstants.START_METHOD, "createExam");
                Exam exam = Exam.builder()
                                .name(request.getName())
                                .code(request.getCode().toUpperCase())
                                .description(request.getDescription())
                                .iconUrl(request.getIconUrl())
                                .build();
                Exam saved = examRepository.save(exam);
                log.info(LogConstants.END_METHOD, "createExam");
                return toExamResponse(saved);
        }

        public List<ExamResponse> getAllExams() {
                log.info(LogConstants.START_METHOD, "getAllExams");
                List<ExamResponse> exams = examRepository.findAll().stream()
                                .map(this::toExamResponse)
                                .collect(Collectors.toList());
                log.info(LogConstants.DATA_LOADED, exams.size(), "Exam");
                log.info(LogConstants.END_METHOD, "getAllExams");
                return exams;
        }

        public ExamResponse getExamById(Long id) {
                return toExamResponse(findExamById(id));
        }

        public Exam findExamById(Long id) {
                return examRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));
        }

        @Transactional
        public void deleteExam(Long id) {
                log.info(LogConstants.START_PROCESS, "deleteExam", id);
                Exam exam = findExamById(id);
                examRepository.delete(exam);
                log.info(LogConstants.COMPLETED_PROCESS, "deleteExam", id);
        }

        // --- Exam Stage Logic ---

        @Transactional
        public ExamStageResponse createStage(CreateExamStageRequest request) {
                log.info(LogConstants.START_METHOD, "createStage");
                Exam exam = findExamById(request.getExamId());
                ExamStage stage = ExamStage.builder()
                                .exam(exam)
                                .name(request.getName())
                                .description(request.getDescription())
                                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                                .build();
                ExamStage saved = examStageRepository.save(stage);
                log.info(LogConstants.END_METHOD, "createStage");
                return toStageResponse(saved);
        }

        public List<ExamStageResponse> getStagesByExamId(Long examId) {
                log.info(LogConstants.START_PROCESS, "getStagesByExamId", examId);
                List<ExamStageResponse> stages = examStageRepository.findByExamIdOrderByOrderIndexAsc(examId).stream()
                                .map(this::toStageResponse)
                                .collect(Collectors.toList());
                log.info(LogConstants.DATA_LOADED, stages.size(), "ExamStage");
                return stages;
        }

        public ExamStage findStageById(Long id) {
                return examStageRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Exam stage not found with id: " + id));
        }

        @Transactional
        public void deleteStage(Long id) {
                log.info(LogConstants.START_PROCESS, "deleteStage", id);
                ExamStage stage = findStageById(id);
                examStageRepository.delete(stage);
                log.info(LogConstants.COMPLETED_PROCESS, "deleteStage", id);
        }

        // --- Exam Year Logic ---

        @Transactional
        public ExamYearResponse createExamYear(CreateExamYearRequest request) {
                log.info(LogConstants.START_METHOD, "createExamYear");
                ExamStage stage = findStageById(request.getExamStageId());
                ExamYear examYear = ExamYear.builder()
                                .examStage(stage)
                                .year(request.getYear())
                                .totalCandidates(request.getTotalCandidates() != null ? request.getTotalCandidates()
                                                : 0L)
                                .totalMarks(request.getTotalMarks())
                                .timeMinutes(request.getTimeMinutes())
                                .build();
                ExamYear saved = examYearRepository.save(examYear);
                log.info(LogConstants.END_METHOD, "createExamYear");
                return toYearResponse(saved);
        }

        public List<ExamYearResponse> getYearsByStageId(Long stageId) {
                log.info(LogConstants.START_PROCESS, "getYearsByStageId", stageId);
                List<ExamYearResponse> years = examYearRepository.findByExamStageIdOrderByYearDesc(stageId).stream()
                                .map(this::toYearResponse)
                                .collect(Collectors.toList());
                log.info(LogConstants.DATA_LOADED, years.size(), "ExamYear");
                return years;
        }

        public ExamYear findExamYearById(Long id) {
                return examYearRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Exam year not found with id: " + id));
        }

        /**
         * Returns a response DTO for a single ExamYear — avoids fetching all years for
         * the stage.
         */
        public ExamYearResponse getExamYearById(Long id) {
                return toYearResponse(findExamYearById(id));
        }

        @Transactional
        public ExamYearResponse updateTotalCandidates(Long id, Long totalCandidates) {
                log.info("Updating total candidates for examYearId={} to {}", id, totalCandidates);
                ExamYear ey = findExamYearById(id);
                ey.setTotalCandidates(totalCandidates);
                return toYearResponse(examYearRepository.save(ey));
        }

        @Transactional
        public void deleteExamYear(Long id) {
                log.info(LogConstants.START_PROCESS, "deleteExamYear", id);
                ExamYear ey = findExamYearById(id);
                examYearRepository.delete(ey);
                log.info(LogConstants.COMPLETED_PROCESS, "deleteExamYear", id);
        }

        // --- Shift Logic ---

        @Transactional
        public Shift createShift(CreateShiftRequest request) {
                log.info(LogConstants.START_METHOD, "createShift");
                ExamYear examYear = findExamYearById(request.getExamYearId());
                Shift shift = Shift.builder()
                                .examYear(examYear)
                                .name(request.getName())
                                .shiftDate(request.getShiftDate())
                                .startTime(request.getStartTime())
                                .endTime(request.getEndTime())
                                .build();
                Shift saved = shiftRepository.save(shift);
                log.info(LogConstants.END_METHOD, "createShift");
                return saved;
        }

        public List<Shift> getShiftsByExamYearId(Long examYearId) {
                log.info(LogConstants.START_PROCESS, "getShiftsByExamYearId", examYearId);
                List<Shift> shifts = shiftRepository.findByExamYearIdOrderByNameAsc(examYearId);
                log.info(LogConstants.DATA_LOADED, shifts.size(), "Shift");
                return shifts;
        }

        public Shift findShiftById(Long id) {
                return shiftRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + id));
        }

        // --- Mappers ---

        private ExamResponse toExamResponse(Exam exam) {
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

        private ExamStageResponse toStageResponse(ExamStage stage) {
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

        private ExamYearResponse toYearResponse(ExamYear ey) {
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

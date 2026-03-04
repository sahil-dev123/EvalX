package com.evalx.config;

import com.evalx.dto.request.*;
import com.evalx.entity.QuestionType;
import com.evalx.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ExamService examService;
    private final ExamStageService examStageService;
    private final ExamYearService examYearService;
    private final SectionService sectionService;
    private final ShiftService shiftService;
    private final MarkingPolicyService markingPolicyService;
    private final QuestionService questionService;

    @Override
    public void run(String... args) {
        if (!examService.getAllExams().isEmpty()) {
            log.info("Data already exists, skipping seed.");
            return;
        }
        log.info("Seeding SSC CGL exam data...");
        seedSSCCGL();
        log.info("Seed complete.");
    }

    private void seedSSCCGL() {
        // Create Exam
        var exam = examService.createExam(CreateExamRequest.builder()
                .name("SSC CGL")
                .code("SSC-CGL")
                .description("Staff Selection Commission - Combined Graduate Level Examination")
                .build());

        // Create Stages
        var pre = examStageService.createStage(CreateExamStageRequest.builder()
                .examId(exam.getId())
                .name("Pre (Tier 1)")
                .description("Preliminary Examination - Computer Based Test")
                .orderIndex(1)
                .build());

        var mains = examStageService.createStage(CreateExamStageRequest.builder()
                .examId(exam.getId())
                .name("Mains (Tier 2)")
                .description("Mains Examination - Computer Based Test")
                .orderIndex(2)
                .build());

        // Create Year for Pre
        var preYear = examYearService.createExamYear(CreateExamYearRequest.builder()
                .examStageId(pre.getId())
                .year(2026)
                .totalCandidates(2500000L)
                .totalMarks(200.0)
                .timeMinutes(60)
                .build());

        // Create Shift for Pre
        var preShift = shiftService.createShift(CreateShiftRequest.builder()
                .examYearId(preYear.getId())
                .name("Shift 1")
                .build());

        // Create Sections for Pre
        String[] sectionNames = {
                "General Intelligence and Reasoning",
                "General Awareness",
                "Quantitative Aptitude",
                "English Comprehension"
        };

        Long[] sectionIds = new Long[4];
        for (int i = 0; i < sectionNames.length; i++) {
            var section = sectionService.createSection(CreateSectionRequest.builder()
                    .shiftId(preShift.getId())
                    .name(sectionNames[i])
                    .totalQuestions(25)
                    .orderIndex(i + 1)
                    .build());
            sectionIds[i] = section.getId();
        }

        // Create Marking Policy (exam-year level: +2, -0.5, 0)
        markingPolicyService.createPolicy(CreateMarkingPolicyRequest.builder()
                .examYearId(preYear.getId())
                .correctMarks(2.0)
                .negativeMarks(0.5)
                .unattemptedMarks(0.0)
                .build());

        // Create Questions with Answer Keys (100 questions, 25 per section)
        String[] answers = {"A", "B", "C", "D"};
        for (int s = 0; s < 4; s++) {
            List<BulkQuestionRequest.QuestionItem> items = new ArrayList<>();
            for (int q = 1; q <= 25; q++) {
                long qNum = (long) (s * 25 + q);
                items.add(BulkQuestionRequest.QuestionItem.builder()
                        .questionNumber(qNum)
                        .questionHash(com.evalx.util.HashUtil.generateHash(String.valueOf(qNum)))
                        .questionText("Mock Question Text for Q" + qNum)
                        .questionType("MCQ")
                        .correctAnswer(answers[(s * 25 + q - 1) % 4])
                        .build());
            }
            questionService.bulkCreateQuestions(BulkQuestionRequest.builder()
                    .sectionId(sectionIds[s])
                    .questions(items)
                    .build());
        }

        // Create Year for Mains (empty — admin can configure)
        examYearService.createExamYear(CreateExamYearRequest.builder()
                .examStageId(mains.getId())
                .year(2026)
                .totalCandidates(500000L)
                .totalMarks(500.0)
                .timeMinutes(120)
                .build());

        log.info("SSC CGL Pre (Tier 1) 2026 seeded with {} questions", 100);
    }
}

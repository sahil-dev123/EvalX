package com.evalx.config;

import com.evalx.constants.ExamConstants;
import com.evalx.dto.request.*;
import com.evalx.service.*;
import com.evalx.util.HashUtil;
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

        private final ExamManagementService examManagementService;
        private final SectionService sectionService;
        private final MarkingPolicyService markingPolicyService;
        private final QuestionService questionService;

        @Override
        public void run(String... args) {
                // Skip seeding if data already exists
                if (!examManagementService.getAllExams().isEmpty()) {
                        log.info("Data already exists, skipping seed.");
                        return;
                }
                log.info("Seeding SSC CGL exam data...");
                seedSSCCGL();
                log.info("Seed complete.");
        }

        private void seedSSCCGL() {
                // Create Exam
                var exam = examManagementService.createExam(CreateExamRequest.builder()
                                .name("SSC CGL")
                                .code("SSC-CGL")
                                .description("Staff Selection Commission - Combined Graduate Level Examination")
                                .build());

                // Create Stages: Pre (Tier 1) and Mains (Tier 2)
                var pre = examManagementService.createStage(CreateExamStageRequest.builder()
                                .examId(exam.getId())
                                .name("Pre (Tier 1)")
                                .description("Preliminary Examination - Computer Based Test")
                                .orderIndex(1)
                                .build());

                var mains = examManagementService.createStage(CreateExamStageRequest.builder()
                                .examId(exam.getId())
                                .name("Mains (Tier 2)")
                                .description("Mains Examination - Computer Based Test")
                                .orderIndex(2)
                                .build());

                // Create Year for Pre using the configured default year constant
                var preYear = examManagementService.createExamYear(CreateExamYearRequest.builder()
                                .examStageId(pre.getId())
                                .year(ExamConstants.DEFAULT_YEAR)
                                .totalCandidates(2500000L)
                                .totalMarks(200.0)
                                .timeMinutes(60)
                                .build());

                // Create Shift for Pre using the default shift name constant
                var preShift = examManagementService.createShift(CreateShiftRequest.builder()
                                .examYearId(preYear.getId())
                                .name(ExamConstants.DEFAULT_SHIFT)
                                .build());

                // Create Sections for Pre (25 questions each)
                String[] sectionNames = {
                                "General Intelligence and Reasoning",
                                "General Awareness",
                                "Quantitative Aptitude",
                                "English Comprehension"
                };

                Long[] sectionIds = new Long[sectionNames.length];
                for (int i = 0; i < sectionNames.length; i++) {
                        var section = sectionService.createSection(CreateSectionRequest.builder()
                                        .shiftId(preShift.getId())
                                        .name(sectionNames[i])
                                        .totalQuestions(25)
                                        .orderIndex(i + 1)
                                        .build());
                        sectionIds[i] = section.getId();
                }

                // Create exam-year-level marking policy: +2 correct, -0.5 negative, 0
                // unattempted
                markingPolicyService.createPolicy(CreateMarkingPolicyRequest.builder()
                                .examYearId(preYear.getId())
                                .correctMarks(2.0)
                                .negativeMarks(0.5)
                                .unattemptedMarks(ExamConstants.DEFAULT_NEGATIVE_MARK)
                                .build());

                // Create 100 mock questions (25 per section) with cycling answers A→B→C→D
                String[] answers = { "A", "B", "C", "D" };
                for (int s = 0; s < sectionNames.length; s++) {
                        List<BulkQuestionRequest.QuestionItem> items = new ArrayList<>();
                        for (int q = 1; q <= 25; q++) {
                                long qNum = (long) (s * 25 + q);
                                items.add(BulkQuestionRequest.QuestionItem.builder()
                                                .questionNumber(qNum)
                                                .questionHash(HashUtil.generateHash(String.valueOf(qNum)))
                                                .questionText("Mock Question Text for Q" + qNum)
                                                .questionType(ExamConstants.QUESTION_TYPE_MCQ)
                                                .correctAnswer(answers[(s * 25 + q - 1) % 4])
                                                .build());
                        }
                        questionService.bulkCreateQuestions(BulkQuestionRequest.builder()
                                        .sectionId(sectionIds[s])
                                        .questions(items)
                                        .build());
                }

                // Create Mains year (empty — admin configures questions separately)
                examManagementService.createExamYear(CreateExamYearRequest.builder()
                                .examStageId(mains.getId())
                                .year(ExamConstants.DEFAULT_YEAR)
                                .totalCandidates(500000L)
                                .totalMarks(500.0)
                                .timeMinutes(120)
                                .build());

                log.info("SSC CGL Pre ({}) {} seeded with {} questions",
                                ExamConstants.DEFAULT_STAGE, ExamConstants.DEFAULT_YEAR, 100);
        }
}

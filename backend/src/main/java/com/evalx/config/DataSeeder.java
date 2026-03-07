package com.evalx.config;

import com.evalx.entity.*;
import com.evalx.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ExamRepository examRepository;
    private final ExamStageRepository examStageRepository;
    private final ExamYearRepository examYearRepository;
    private final MarkingPolicyRepository markingPolicyRepository;
    private final QuestionTypePolicyRepository questionTypePolicyRepository;

    @Override
    public void run(String... args) {
        log.info("DATA_SEEDER: Checking if initial data seeding is required...");
        if (examRepository.count() == 0) {
            log.info("SEEDING_FLOW: Starting database seeding for GATE exam...");

            // 1. Marking Policy
            MarkingPolicy policy = MarkingPolicy.builder()
                    .description("GATE Standard Marking Policy")
                    .build();
            policy = markingPolicyRepository.save(policy);
            log.info("SEED_POLICY: Created MarkingPolicy ID: {}", policy.getId());

            // 2. Exam
            Exam gate = Exam.builder()
                    .name("GATE")
                    .code("GATE")
                    .build();
            gate = examRepository.save(gate);
            log.info("SEED_EXAM: Created Exam '{}' (ID: {})", gate.getCode(), gate.getId());

            // 3. Stage
            ExamStage stage = ExamStage.builder()
                    .exam(gate)
                    .name("Paper 1")
                    .description("Primary Question Paper")
                    .build();
            stage = examStageRepository.save(stage);
            log.info("SEED_STAGE: Created Stage '{}' (ID: {})", stage.getName(), stage.getId());

            // 4. Year
            ExamYear year = ExamYear.builder()
                    .stage(stage)
                    .year(2024)
                    .totalMarks(100.0)
                    .timeMinutes(180)
                    .markingPolicy(policy)
                    .build();
            year = examYearRepository.save(year);
            log.info("SEED_YEAR: Created Year {} (ID: {})", year.getYear(), year.getId());

            // 5. Question Type Policies
            log.info("SEED_RULES: Populating question type marks rules...");
            questionTypePolicyRepository.save(QuestionTypePolicy.builder()
                    .exam(gate)
                    .questionType("MCQ")
                    .marks(1.0)
                    .negativeMarks(0.33)
                    .build());

            questionTypePolicyRepository.save(QuestionTypePolicy.builder()
                    .exam(gate)
                    .questionType("NAT")
                    .marks(1.0)
                    .negativeMarks(0.0)
                    .build());

            log.info("DATA_SEEDER_COMPLETE: Seeding finished successfully.");
        } else {
            log.info("DATA_SEEDER_SKIP: Database already contains {} exams. Skipping seeding.", examRepository.count());
        }
    }
}

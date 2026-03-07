package com.evalx.service;

import com.evalx.entity.Exam;
import com.evalx.entity.MarkingPolicy;
import com.evalx.entity.QuestionTypePolicy;
import com.evalx.repository.ExamRepository;
import com.evalx.repository.MarkingPolicyRepository;
import com.evalx.repository.QuestionTypePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamService {

    private final ExamRepository examRepository;
    private final MarkingPolicyRepository markingPolicyRepository;
    private final QuestionTypePolicyRepository questionTypePolicyRepository;

    public List<Exam> getAllExams() {
        log.info("ExamService.getAllExams called");
        List<Exam> exams = examRepository.findAll();
        log.info("Found {} exams in DB", exams.size());
        return exams;
    }

    @Transactional
    public void seedInitialData() {
        if (examRepository.count() == 0) {
            log.info("Seeding initial exam data via Service...");
            MarkingPolicy policy = MarkingPolicy.builder()
                    .description("GATE Standard Marking Policy")
                    .build();
            policy = markingPolicyRepository.save(policy);

            Exam gate = Exam.builder()
                    .name("GATE")
                    .code("GATE")
                    .markingPolicy(policy)
                    .build();
            gate = examRepository.save(gate);

            questionTypePolicyRepository.save(QuestionTypePolicy.builder()
                    .exam(gate)
                    .questionType("MCQ_1")
                    .marks(1.0)
                    .negativeMarks(0.33)
                    .build());

            log.info("Successfully seeded GATE exam data via Service.");
        }
    }

    public Exam getExamById(Long id) {
        return examRepository.findById(id).orElseThrow();
    }
}

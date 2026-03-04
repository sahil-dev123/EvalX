package com.evalx.repository;

import com.evalx.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySectionIdOrderByQuestionNumberAsc(Long sectionId);

    @Query("SELECT q FROM Question q WHERE q.section.examYear.id = :examYearId ORDER BY q.section.orderIndex, q.questionNumber")
    List<Question> findByExamYearId(Long examYearId);

    Optional<Question> findBySectionIdAndQuestionNumber(Long sectionId, Long questionNumber);

    @Query("SELECT q FROM Question q JOIN FETCH q.answerKey WHERE q.section.examYear.id = :examYearId")
    List<Question> findWithAnswerKeyByExamYearId(Long examYearId);
}

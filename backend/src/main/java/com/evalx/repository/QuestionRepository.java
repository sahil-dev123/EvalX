package com.evalx.repository;

import com.evalx.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySectionIdOrderByQuestionNumberAsc(Long sectionId);

    @Query("SELECT q FROM Question q WHERE q.section.shift.examYear.id = :examYearId ORDER BY q.section.orderIndex, q.questionNumber")
    List<Question> findByExamYearId(Long examYearId);

    Optional<Question> findBySectionIdAndQuestionNumber(Long sectionId, Long questionNumber);

    @Query("SELECT q FROM Question q JOIN FETCH q.answerKey WHERE q.section.shift.examYear.id = :examYearId")
    List<Question> findWithAnswerKeyByExamYearId(Long examYearId);

    @Query("SELECT q.section.shift.id FROM Question q WHERE q.questionHash IN :hashes GROUP BY q.section.shift.id ORDER BY COUNT(q) DESC LIMIT 1")
    Optional<Long> findMostLikelyShiftIdByHashes(@org.springframework.data.repository.query.Param("hashes") java.util.Collection<String> hashes);
    
    @Query("SELECT q FROM Question q JOIN FETCH q.answerKey WHERE q.section.shift.id = :shiftId")
    List<Question> findWithAnswerKeyByShiftId(Long shiftId);
}

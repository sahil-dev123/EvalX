package com.evalx.repository;

import com.evalx.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamYearId(Long examYearId);

    // Legacy support (optional, or remove if truly unused)
    // List<Question> findByExamId(Long examId);
}

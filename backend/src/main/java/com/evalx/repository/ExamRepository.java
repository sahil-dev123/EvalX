package com.evalx.repository;

import com.evalx.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    Optional<Exam> findByCode(String code);
    boolean existsByCode(String code);
}

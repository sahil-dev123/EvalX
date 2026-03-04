package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marking_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarkingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_year_id", nullable = false)
    private ExamYear examYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "correct_marks", nullable = false)
    private Double correctMarks;

    @Column(name = "negative_marks", nullable = false)
    @Builder.Default
    private Double negativeMarks = 0.0;

    @Column(name = "unattempted_marks")
    @Builder.Default
    private Double unattemptedMarks = 0.0;
}

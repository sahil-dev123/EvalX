package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_year_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    @JsonIgnore
    private ExamStage stage;

    @Column(name = "exam_year", nullable = false)
    private Integer year;

    @Column(name = "total_marks")
    private Double totalMarks;

    @Column(name = "time_minutes")
    private Integer timeMinutes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "marking_policy_id")
    private MarkingPolicy markingPolicy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

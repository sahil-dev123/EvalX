package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_years")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_stage_id", nullable = false)
    private ExamStage examStage;

    @Column(name = "exam_year", nullable = false)
    private Integer year;

    @Column(name = "total_candidates")
    @Builder.Default
    private Long totalCandidates = 0L;

    @Column(name = "total_marks")
    private Double totalMarks;

    @Column(name = "time_minutes")
    private Integer timeMinutes;

    @OneToMany(mappedBy = "examYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Shift> shifts = new ArrayList<>();

    @OneToMany(mappedBy = "examYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarkingPolicy> markingPolicies = new ArrayList<>();
}

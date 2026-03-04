package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluation_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private ResponseSubmission submission;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    @Column(nullable = false)
    private Integer correct;

    @Column(nullable = false)
    private Integer incorrect;

    @Column(nullable = false)
    private Integer skipped;

    private Double percentile;

    @Column(name = "estimated_rank")
    private Long estimatedRank;

    @Column(name = "z_score")
    private Double zScore;

    @OneToMany(mappedBy = "evaluationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SectionScore> sectionScores = new ArrayList<>();

    @Column(name = "evaluated_at")
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}

package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "section_scores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SectionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_result_id", nullable = false)
    private EvaluationResult evaluationResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    private Double score;

    @Column(name = "max_score")
    private Double maxScore;

    private Integer correct;
    private Integer incorrect;
    private Integer skipped;
}

package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "score_distributions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoreDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_year_id", nullable = false)
    private ExamYear examYear;

    @Column(name = "score_bucket", nullable = false)
    private String scoreBucket;

    @Column(nullable = false)
    @Builder.Default
    private Long frequency = 0L;
}

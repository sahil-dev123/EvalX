package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_stages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @OneToMany(mappedBy = "examStage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamYear> examYears = new ArrayList<>();
}

package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_id")
    private String paperId;

    @Column(name = "display_question_number")
    private Integer displayQuestionNumber;

    @Column(name = "actual_question_number")
    private Integer actualQuestionNumber;
}

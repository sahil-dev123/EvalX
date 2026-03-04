package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answer_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnswerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;
}

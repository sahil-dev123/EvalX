package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_question_hash", columnList = "question_hash")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    private Long questionNumber;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String options; // Stored as JSON or delimited string

    @Column(name = "question_hash", length = 64)
    private String questionHash; // SHA-256 hash
    
    @Column(name = "question_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuestionType questionType = QuestionType.MCQ;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnswerKey answerKey;
}

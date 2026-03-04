package com.evalx.engine;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Score {
    @Builder.Default
    private double totalScore = 0;
    @Builder.Default
    private int correct = 0;
    @Builder.Default
    private int incorrect = 0;
    @Builder.Default
    private int skipped = 0;

    public void addCorrect(double marks) {
        this.totalScore += marks;
        this.correct++;
    }

    public void addIncorrect(double marks) {
        this.totalScore -= marks;
        this.incorrect++;
    }

    public void addSkipped(double marks) {
        this.totalScore += marks;
        this.skipped++;
    }

    public void merge(Score other) {
        this.totalScore += other.totalScore;
        this.correct += other.correct;
        this.incorrect += other.incorrect;
        this.skipped += other.skipped;
    }
}

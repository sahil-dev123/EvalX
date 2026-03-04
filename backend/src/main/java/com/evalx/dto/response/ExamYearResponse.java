package com.evalx.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamYearResponse {
    private Long id;
    private Long examStageId;
    private String stageName;
    private String examName;
    private Integer year;
    private Long totalCandidates;
    private Double totalMarks;
    private Integer timeMinutes;
    private List<SectionInfo> sections;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SectionInfo {
        private Long id;
        private String name;
        private Integer totalQuestions;
        private Integer orderIndex;
    }
}

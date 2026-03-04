package com.evalx.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamStageResponse {
    private Long id;
    private Long examId;
    private String examName;
    private String name;
    private String description;
    private Integer orderIndex;
    private List<YearInfo> years;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class YearInfo {
        private Long id;
        private Integer year;
        private Long totalCandidates;
    }
}

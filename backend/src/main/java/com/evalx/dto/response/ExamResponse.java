package com.evalx.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String iconUrl;
    private List<StageInfo> stages;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StageInfo {
        private Long id;
        private String name;
        private String description;
        private Integer orderIndex;
    }
}

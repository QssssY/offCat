package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInterviewResponse {

    private String sessionId;
    private String jobRole;
    private String difficultyDesc;
    private String interviewMode;
    private String statusDesc;
    private Integer comprehensiveScore;
    private LocalDateTime createTime;
}

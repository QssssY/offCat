package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResumeTaskResponse {

    private Long id;
    private String statusDesc;
    private String errorMsg;
    private LocalDateTime createTime;
}

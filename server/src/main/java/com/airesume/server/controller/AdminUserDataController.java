package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.UserInterviewResponse;
import com.airesume.server.dto.admin.UserResumeTaskResponse;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserDataController {

    private final InterviewSessionMapper interviewSessionMapper;
    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;

    @GetMapping("/{userId}/interviews")
    public Result<Map<String, Object>> getUserInterviews(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          Authentication authentication) {
        log.info("Admin get user interviews, userId: {}, page: {}, size: {}", userId, page, size);

        Page<InterviewSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreateTime);
        Page<InterviewSession> result = interviewSessionMapper.selectPage(pageParam, wrapper);

        List<UserInterviewResponse> records = result.getRecords().stream()
                .map(this::buildInterviewResponse)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    @GetMapping("/{userId}/resume-tasks")
    public Result<Map<String, Object>> getUserResumeTasks(@PathVariable Long userId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           Authentication authentication) {
        log.info("Admin get user resume tasks, userId: {}, page: {}, size: {}", userId, page, size);

        Page<ResumeDiagnosisTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ResumeDiagnosisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getUserId, userId)
                .orderByDesc(ResumeDiagnosisTask::getCreateTime);
        Page<ResumeDiagnosisTask> result = resumeDiagnosisTaskMapper.selectPage(pageParam, wrapper);

        List<UserResumeTaskResponse> records = result.getRecords().stream()
                .map(this::buildResumeTaskResponse)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    private UserInterviewResponse buildInterviewResponse(InterviewSession session) {
        String difficultyDesc = switch (session.getDifficulty()) {
            case 1 -> "初级";
            case 2 -> "中级";
            case 3 -> "高级";
            default -> "未知";
        };
        String statusDesc = session.getStatus() == 1 ? "已结束" : "进行中";
        String interviewMode = session.getInterviewMode() != null ? session.getInterviewMode() : "normal";
        return UserInterviewResponse.builder()
                .sessionId(session.getSessionId())
                .jobRole(session.getJobRole())
                .difficultyDesc(difficultyDesc)
                .interviewMode(interviewMode)
                .statusDesc(statusDesc)
                .comprehensiveScore(session.getComprehensiveScore())
                .createTime(session.getCreateTime())
                .build();
    }

    private UserResumeTaskResponse buildResumeTaskResponse(ResumeDiagnosisTask task) {
        String statusDesc = switch (task.getStatus()) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
        return UserResumeTaskResponse.builder()
                .id(task.getId())
                .statusDesc(statusDesc)
                .errorMsg(task.getErrorMsg())
                .createTime(task.getCreateTime())
                .build();
    }
}

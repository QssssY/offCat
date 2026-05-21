package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.onboarding.OnboardingStatusResponse;
import com.airesume.server.dto.onboarding.OnboardingTasksResponse;
import com.airesume.server.dto.onboarding.OnboardingUpdateRequest;
import com.airesume.server.entity.UserOnboardingState;
import com.airesume.server.entity.UserOnboardingTask;
import com.airesume.server.mapper.UserOnboardingStateMapper;
import com.airesume.server.mapper.UserOnboardingTaskMapper;
import com.airesume.server.service.UserOnboardingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户新手引导服务实现类
 * 管理用户引导状态的查询、进度更新、完成和跳过
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnboardingServiceImpl extends ServiceImpl<UserOnboardingStateMapper, UserOnboardingState>
        implements UserOnboardingService {

    private final UserOnboardingTaskMapper onboardingTaskMapper;

    /** 合法的状态值 */
    private static final String STATUS_NOT_STARTED = "not_started";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_SKIPPED = "skipped";

    /** 允许通过 API 设置的状态（终态和进行中） */
    private static final List<String> VALID_UPDATE_STATUSES = Arrays.asList(
            STATUS_IN_PROGRESS, STATUS_COMPLETED, STATUS_SKIPPED
    );

    /** 新手任务定义（保持插入顺序） */
    private static final LinkedHashMap<String, TaskDefinition> TASK_DEFINITIONS = new LinkedHashMap<>();

    static {
        TASK_DEFINITIONS.put("resume_uploaded",
                new TaskDefinition("上传简历", "上传你的第一份简历", "/resume/upload"));
        TASK_DEFINITIONS.put("report_viewed",
                new TaskDefinition("查看诊断报告", "查看 AI 诊断报告", "/resume/history"));
        TASK_DEFINITIONS.put("jd_compared",
                new TaskDefinition("岗位匹配分析", "完成一次岗位 JD 对比", "/resume/history"));
        TASK_DEFINITIONS.put("interview_completed",
                new TaskDefinition("完成模拟面试", "完成一次模拟面试", "/interview/entry"));
    }

    /** 合法 taskKey 集合（快速校验用） */
    private static final Set<String> VALID_TASK_KEYS = TASK_DEFINITIONS.keySet();

    /** 任务定义内部类 */
    private record TaskDefinition(String label, String desc, String actionUrl) {}

    @Override
    public OnboardingStatusResponse getStatus(Long userId, String guideKey) {
        log.debug("查询引导状态，userId: {}, guideKey: {}", userId, guideKey);

        // 查询用户的引导状态记录
        UserOnboardingState state = getStateByUserAndKey(userId, guideKey);

        if (state == null) {
            // 无记录，返回默认的 not_started 状态
            log.debug("未找到引导记录，返回默认状态，userId: {}", userId);
            return OnboardingStatusResponse.builder()
                    .guideKey(guideKey)
                    .status(STATUS_NOT_STARTED)
                    .currentStep(0)
                    .showGuide(true)
                    .build();
        }

        // 有记录，根据状态判断是否需要展示引导
        boolean showGuide = STATUS_NOT_STARTED.equals(state.getStatus())
                || STATUS_IN_PROGRESS.equals(state.getStatus());

        log.debug("引导状态查询完成，userId: {}, status: {}, currentStep: {}, showGuide: {}",
                userId, state.getStatus(), state.getCurrentStep(), showGuide);

        return OnboardingStatusResponse.builder()
                .guideKey(state.getGuideKey())
                .status(state.getStatus())
                .currentStep(state.getCurrentStep())
                .showGuide(showGuide)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, OnboardingUpdateRequest request) {
        String guideKey = request.getGuideKey();
        String newStatus = request.getStatus();
        Integer currentStep = request.getCurrentStep();

        log.info("更新引导状态，userId: {}, guideKey: {}, status: {}, currentStep: {}",
                userId, guideKey, newStatus, currentStep);

        // 校验状态值合法性
        if (!VALID_UPDATE_STATUSES.contains(newStatus)) {
            log.warn("无效的引导状态值: {}", newStatus);
            throw new BusinessException("无效的引导状态值: " + newStatus);
        }

        // 进行中状态必须携带步骤信息
        if (STATUS_IN_PROGRESS.equals(newStatus) && currentStep == null) {
            log.warn("进行中状态缺少 currentStep 参数，userId: {}", userId);
            throw new BusinessException("进行中状态必须提供当前步骤");
        }

        // 查询已有记录
        UserOnboardingState existingState = getStateByUserAndKey(userId, guideKey);

        if (existingState == null) {
            // 首次记录，创建新状态（处理并发请求的唯一索引冲突）
            try {
                createNewState(userId, guideKey, newStatus, currentStep);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发请求导致唯一索引冲突，重新查询并更新
                log.debug("引导状态记录并发创建冲突，重新查询更新, userId: {}, guideKey: {}", userId, guideKey);
                existingState = getStateByUserAndKey(userId, guideKey);
                if (existingState != null) {
                    updateExistingState(existingState, newStatus, currentStep);
                }
            }
            return;
        }

        // 已完成或已跳过的记录不允许重新打开（幂等处理）
        if (STATUS_COMPLETED.equals(existingState.getStatus())
                || STATUS_SKIPPED.equals(existingState.getStatus())) {
            log.info("引导已完成或已跳过，忽略更新请求，userId: {}, currentStatus: {}",
                    userId, existingState.getStatus());
            return;
        }

        // 更新已有记录
        updateExistingState(existingState, newStatus, currentStep);
    }

    /**
     * 根据用户ID和引导标识查询状态记录
     */
    private UserOnboardingState getStateByUserAndKey(Long userId, String guideKey) {
        LambdaQueryWrapper<UserOnboardingState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOnboardingState::getUserId, userId)
                .eq(UserOnboardingState::getGuideKey, guideKey);
        return getOne(wrapper);
    }

    /**
     * 创建新的引导状态记录
     */
    private void createNewState(Long userId, String guideKey, String status, Integer currentStep) {
        UserOnboardingState newState = new UserOnboardingState();
        newState.setUserId(userId);
        newState.setGuideKey(guideKey);
        newState.setStatus(status);
        newState.setCurrentStep(currentStep != null ? currentStep : 0);

        // 设置终态时间
        if (STATUS_COMPLETED.equals(status)) {
            newState.setCompletedTime(LocalDateTime.now());
        } else if (STATUS_SKIPPED.equals(status)) {
            newState.setSkipTime(LocalDateTime.now());
        }

        save(newState);
        log.info("创建引导状态记录成功，userId: {}, status: {}", userId, status);
    }

    /**
     * 更新已有的引导状态记录
     */
    private void updateExistingState(UserOnboardingState state, String newStatus, Integer currentStep) {
        state.setStatus(newStatus);
        if (currentStep != null) {
            state.setCurrentStep(currentStep);
        }

        // 设置终态时间
        if (STATUS_COMPLETED.equals(newStatus)) {
            state.setCompletedTime(LocalDateTime.now());
        } else if (STATUS_SKIPPED.equals(newStatus)) {
            state.setSkipTime(LocalDateTime.now());
        }

        updateById(state);
        log.info("更新引导状态成功，userId: {}, newStatus: {}", state.getUserId(), newStatus);
    }

    // ============================== 新手任务式引导 ==============================

    @Override
    public OnboardingTasksResponse getTasks(Long userId) {
        log.debug("查询新手任务列表，userId: {}", userId);

        // 旧引导已完成/已跳过的用户不展示任务卡片
        UserOnboardingState guideState = getStateByUserAndKey(userId, DEFAULT_GUIDE_KEY);
        if (guideState != null && (STATUS_COMPLETED.equals(guideState.getStatus())
                || STATUS_SKIPPED.equals(guideState.getStatus()))) {
            log.debug("旧引导已完成或已跳过，不展示任务卡片，userId: {}", userId);
            return buildInvisibleResponse();
        }

        // 查询该用户已完成的任务记录
        List<UserOnboardingTask> completedRecords = onboardingTaskMapper.selectList(
                new LambdaQueryWrapper<UserOnboardingTask>()
                        .eq(UserOnboardingTask::getUserId, userId)
                        .eq(UserOnboardingTask::getCompleted, 1));
        Set<String> completedKeys = completedRecords.stream()
                .map(UserOnboardingTask::getTaskKey)
                .collect(Collectors.toSet());

        // 构建任务列表
        List<OnboardingTasksResponse.TaskItem> tasks = new ArrayList<>();
        for (Map.Entry<String, TaskDefinition> entry : TASK_DEFINITIONS.entrySet()) {
            String key = entry.getKey();
            TaskDefinition def = entry.getValue();
            tasks.add(OnboardingTasksResponse.TaskItem.builder()
                    .taskKey(key)
                    .taskLabel(def.label())
                    .taskDesc(def.desc())
                    .completed(completedKeys.contains(key))
                    .actionUrl(def.actionUrl())
                    .build());
        }

        int completedCount = completedKeys.size();
        int totalCount = TASK_DEFINITIONS.size();
        boolean allCompleted = completedCount >= totalCount;

        return OnboardingTasksResponse.builder()
                .tasks(tasks)
                .completedCount(completedCount)
                .totalCount(totalCount)
                .allCompleted(allCompleted)
                .visible(!allCompleted)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long userId, String taskKey) {
        // 校验 taskKey 合法性
        if (!VALID_TASK_KEYS.contains(taskKey)) {
            throw new BusinessException("无效的任务标识: " + taskKey);
        }

        // 幂等检查：已存在且已完成则直接返回
        UserOnboardingTask existing = onboardingTaskMapper.selectOne(
                new LambdaQueryWrapper<UserOnboardingTask>()
                        .eq(UserOnboardingTask::getUserId, userId)
                        .eq(UserOnboardingTask::getTaskKey, taskKey));
        if (existing != null && existing.getCompleted() == 1) {
            log.debug("任务已完成，幂等返回，userId: {}, taskKey: {}", userId, taskKey);
            return;
        }

        // 插入新记录或更新已有未完成记录
        if (existing == null) {
            try {
                UserOnboardingTask task = new UserOnboardingTask();
                task.setUserId(userId);
                task.setTaskKey(taskKey);
                task.setCompleted(1);
                task.setCompletedTime(LocalDateTime.now());
                onboardingTaskMapper.insert(task);
                log.info("新手任务完成，userId: {}, taskKey: {}", userId, taskKey);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发插入冲突，重新查询后更新
                log.debug("任务记录并发创建冲突，重新查询更新，userId: {}, taskKey: {}", userId, taskKey);
                existing = onboardingTaskMapper.selectOne(
                        new LambdaQueryWrapper<UserOnboardingTask>()
                                .eq(UserOnboardingTask::getUserId, userId)
                                .eq(UserOnboardingTask::getTaskKey, taskKey));
                if (existing != null && existing.getCompleted() != 1) {
                    existing.setCompleted(1);
                    existing.setCompletedTime(LocalDateTime.now());
                    onboardingTaskMapper.updateById(existing);
                }
            }
        } else {
            existing.setCompleted(1);
            existing.setCompletedTime(LocalDateTime.now());
            onboardingTaskMapper.updateById(existing);
            log.info("新手任务标记完成，userId: {}, taskKey: {}", userId, taskKey);
        }
    }

    /** 构建旧用户不可见响应（全部标记已完成） */
    private OnboardingTasksResponse buildInvisibleResponse() {
        List<OnboardingTasksResponse.TaskItem> tasks = TASK_DEFINITIONS.entrySet().stream()
                .map(e -> OnboardingTasksResponse.TaskItem.builder()
                        .taskKey(e.getKey())
                        .taskLabel(e.getValue().label())
                        .taskDesc(e.getValue().desc())
                        .completed(true)
                        .actionUrl(e.getValue().actionUrl())
                        .build())
                .toList();
        int total = TASK_DEFINITIONS.size();
        return OnboardingTasksResponse.builder()
                .tasks(tasks).completedCount(total).totalCount(total)
                .allCompleted(true).visible(false).build();
    }

    /** 旧引导的默认 guideKey */
    private static final String DEFAULT_GUIDE_KEY = "v1_3_main_onboarding";
}

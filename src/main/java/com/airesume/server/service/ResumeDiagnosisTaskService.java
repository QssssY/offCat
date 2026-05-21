package com.airesume.server.service;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历诊断任务服务接口。
 */
public interface ResumeDiagnosisTaskService extends IService<ResumeDiagnosisTask> {

    /**
     * 创建简历诊断任务。
     *
     * @param userId 用户 ID
     * @param fileUrl 简历文件地址
     * @return 任务 ID
     */
    Long createTask(Long userId, String fileUrl);

    /**
     * 创建简历诊断任务，支持文件上传。
     *
     * @param userId 用户 ID
     * @param file PDF 简历文件
     * @return 任务 ID，使用字符串避免前端长整型精度丢失
     */
    String createTask(Long userId, MultipartFile file);

    /**
     * 根据任务 ID 查询任务详情。
     *
     * @param taskId 任务 ID
     * @param userId 用户 ID，用于校验任务归属
     * @return 任务详情
     */
    ResumeDiagnosisTaskResponse getTaskById(Long taskId, Long userId);

    /**
     * 查询用户的简历诊断历史记录，分页返回。
     */
    PageResult<ResumeDiagnosisHistoryResponse> getHistoryByUserId(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询用户的简历诊断历史记录，兼容旧接口。
     */
    @Deprecated
    List<ResumeDiagnosisHistoryResponse> getHistoryByUserId(Long userId);

    /**
     * 原子将任务状态从待处理切换为处理中。
     *
     * @param taskId 任务 ID
     * @return true 表示当前线程抢占成功，false 表示任务已被其他线程处理或不存在
     */
    boolean updateStatusToProcessing(Long taskId);

    /**
     * 更新任务状态为完成。
     */
    void updateStatusToCompleted(Long taskId, String diagnosisResult);

    /**
     * 更新任务状态为失败。
     */
    void updateStatusToFailed(Long taskId, String errorMsg);

    /**
     * 获取状态描述。
     */
    String getStatusDescription(Integer status);

    /**
     * 更新任务缓存的简历文本内容。
     */
    void updateTaskResumeText(Long taskId, String resumeText);

    /**
     * 更新任务缓存的简历文本和解析元信息。
     */
    void updateTaskResumeParseResult(Long taskId, String resumeText, String parseMode, String parseMessage);

    /**
     * 获取任务当前状态。
     */
    Integer getTaskStatus(Long taskId);

    /**
     * 回收长时间卡在处理中状态的孤儿任务。
     *
     * @param timeoutMinutes 超时阈值，单位分钟
     * @return 回收的任务数量
     */
    int recoverOrphanedTasks(int timeoutMinutes);

    /**
     * 清理当前用户的全部简历诊断历史及其衍生记录。
     *
     * @param userId 当前登录用户 ID
     * @return 清理的简历诊断任务数量
     */
    int clearHistory(Long userId);

    /**
     * 删除单条简历诊断记录及其衍生数据。
     *
     * @param userId 当前登录用户 ID
     * @param taskId 任务 ID
     * @return 是否删除成功
     */
    boolean deleteTask(Long userId, Long taskId);

    /**
     * 更新任务子阶段（不影响主状态机）。
     *
     * @param taskId 任务 ID
     * @param stage  子阶段标识：extracting / ai_analyzing / enhancing
     */
    void updateStage(Long taskId, String stage);

    /**
     * 重试失败的诊断任务。
     * 校验：任务归属、状态为失败、失败时间在 24h 内。
     * 行为：复用原文件创建新任务，原任务保留。
     *
     * @param taskId 原失败任务 ID
     * @param userId 当前用户 ID
     * @return 新任务 ID
     */
    String retryFailedTask(Long taskId, Long userId);
}

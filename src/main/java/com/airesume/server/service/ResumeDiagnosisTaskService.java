package com.airesume.server.service;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历诊断任务服务接口
 * 定义简历诊断相关的业务操作
 */
public interface ResumeDiagnosisTaskService extends IService<ResumeDiagnosisTask> {

    /**
     * 创建简历诊断任务
     *
     * @param userId  用户ID
     * @param fileUrl 简历文件地址
     * @return 任务ID
     */
    Long createTask(Long userId, String fileUrl);

    /**
     * 创建简历诊断任务（支持文件上传）
     *
     * @param userId 用户ID
     * @param file   PDF简历文件
     * @return 任务ID（字符串形式，避免前端精度丢失）
     */
    String createTask(Long userId, MultipartFile file);

    /**
     * 根据任务ID查询任务详情
     *
     * @param taskId 任务ID
     * @param userId 用户ID（用于校验权限）
     * @return 任务详情响应
     */
    ResumeDiagnosisTaskResponse getTaskById(Long taskId, Long userId);

    /**
     * 查询用户的简历诊断历史记录（分页）
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页历史记录
     */
    PageResult<ResumeDiagnosisHistoryResponse> getHistoryByUserId(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询用户的简历诊断历史记录（不分页，兼容旧版）
     *
     * @param userId 用户ID
     * @return 历史记录列表
     * @deprecated 请使用分页方法
     */
    @Deprecated
    List<ResumeDiagnosisHistoryResponse> getHistoryByUserId(Long userId);

    /**
     * 更新任务状态为处理中
     *
     * @param taskId 任务ID
     */
    void updateStatusToProcessing(Long taskId);

    /**
     * 更新任务状态为完成
     *
     * @param taskId          任务ID
     * @param diagnosisResult 诊断结果JSON
     */
    void updateStatusToCompleted(Long taskId, String diagnosisResult);

    /**
     * 更新任务状态为失败
     *
     * @param taskId   任务ID
     * @param errorMsg 错误信息
     */
    void updateStatusToFailed(Long taskId, String errorMsg);

    /**
     * 获取状态描述
     *
     * @param status 状态码
     * @return 状态描述
     */
    String getStatusDescription(Integer status);
}

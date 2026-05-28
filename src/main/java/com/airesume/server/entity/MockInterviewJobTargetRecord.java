package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位定向模拟面试记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mock_interview_job_target_record")
public class MockInterviewJobTargetRecord extends BaseEntity {

    /**
     * 用户 ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联的会话 ID。
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 关联的简历诊断任务 ID。
     */
    @TableField("resume_task_id")
    private Long resumeTaskId;

    /**
     * 本次会话实际使用的 JD 文本快照。
     */
    @TableField(value = "jd_text", select = false)
    private String jdText;

    /**
     * 关联的岗位 JD 对比分析记录 ID。
     */
    @TableField("job_match_record_id")
    private Long jobMatchRecordId;

    /**
     * 首轮岗位定向问题快照。
     */
    @TableField(value = "generated_questions", select = false)
    private String generatedQuestions;

    /**
     * 岗位定向反馈结构化 JSON。
     */
    @TableField(value = "job_targeted_feedback", select = false)
    private String jobTargetedFeedback;

    /**
     * 上下文来源类型。
     */
    @TableField("source_type")
    private String sourceType;
}

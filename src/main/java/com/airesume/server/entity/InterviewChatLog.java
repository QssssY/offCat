package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试聊天记录实体类
 * 对应数据库表 interview_chat_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_chat_log")
@Entity
@Table(name = "interview_chat_log")
public class InterviewChatLog extends BaseEntity {

    /**
     * 会话ID，关联interview_session.session_id
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 发送者角色：user / assistant / system
     */
    @TableField("message_role")
    private String messageRole;

    /**
     * 具体的提问或回答内容
     */
    @TableField("content")
    private String content;
}

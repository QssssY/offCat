package com.airesume.server.mock;

import com.airesume.server.dto.interview.InterviewJobTargetContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MockInterviewServiceTest {

    @Test
    void shouldNotExposeRawResumeMetadataInResumeBasedReply() {
        MockInterviewService service = new MockInterviewService();
        InterviewJobTargetContext context = InterviewJobTargetContext.builder()
                .resumeText("""
                        林映文-前端开发工程师求职简历姓名：林映文|性别：男|电话：13800000000
                        项目经历：AI 简历诊断平台，负责前端页面和接口联调。
                        """)
                .build();

        String reply = service.generateMockReply("session-1", "我是林宝荣。", 1, context);

        assertFalse(reply.contains("林映文"));
        assertFalse(reply.contains("求职简历"));
        assertFalse(reply.contains("姓名"));
        assertFalse(reply.contains("性别"));
        assertFalse(reply.contains("围绕“"));
    }

    @Test
    void shouldNotExposeRawResumeMetadataInOpening() {
        MockInterviewService service = new MockInterviewService();
        InterviewJobTargetContext context = InterviewJobTargetContext.builder()
                .resumeText("""
                        前端开发工程师求职简历姓名：候选人|性别：男
                        实习经历：在业务团队参与管理后台开发。
                        """)
                .build();

        String opening = service.generateMockOpening("前端开发工程师", 2, context);

        assertFalse(opening.contains("求职简历"));
        assertFalse(opening.contains("姓名"));
        assertFalse(opening.contains("性别"));
        assertFalse(opening.contains("“前端开发工程师"));
    }
}

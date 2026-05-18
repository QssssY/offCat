package com.airesume.server.controller;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewJobRoleResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.SysJobRole;
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.InterviewService;
import com.airesume.server.service.MockInterviewJobTargetService;
import com.airesume.server.service.SysJobRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private InterviewService interviewService;
    @Mock private InterviewAiService interviewAiService;
    @Mock private SysJobRoleService sysJobRoleService;
    @Mock private MockInterviewJobTargetService mockInterviewJobTargetService;
    @Mock private Executor aiAsyncExecutor;
    @Mock private Authentication authentication;

    private InterviewController controller;

    @BeforeEach
    void setUp() {
        controller = new InterviewController(
                interviewService, interviewAiService, sysJobRoleService,
                mockInterviewJobTargetService, aiAsyncExecutor);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getJobRolesShouldReturnList() {
        SysJobRole role1 = new SysJobRole();
        role1.setRoleCode("java");
        role1.setRoleName("Java工程师");
        role1.setInterviewTag("java_backend");
        role1.setTagType("backend");
        SysJobRole role2 = new SysJobRole();
        role2.setRoleCode("frontend");
        role2.setRoleName("前端工程师");
        role2.setInterviewTag("frontend");
        role2.setTagType("frontend");

        when(sysJobRoleService.listActiveOrdered()).thenReturn(List.of(role1, role2));

        Result<List<InterviewJobRoleResponse>> result = controller.getJobRoles();
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("java", result.getData().get(0).getRoleCode());
    }

    @Test
    void createSessionShouldReturnSessionResponse() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setJobRole("Java工程师");
        request.setDifficulty(2);
        request.setInterviewMode(InterviewConstants.MODE_TECH_LEADER);

        InterviewSessionResponse mockResponse = InterviewSessionResponse.builder()
                .sessionId("session-1")
                .jobRole("Java工程师")
                .interviewMode(InterviewConstants.MODE_TECH_LEADER)
                .interviewModeDesc("技术 Leader 面")
                .build();

        when(interviewService.createSession(eq(1L), eq(request))).thenReturn(mockResponse);

        Result<InterviewSessionResponse> result = controller.createSession(request, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals("session-1", result.getData().getSessionId());
        assertEquals("技术 Leader 面", result.getData().getInterviewModeDesc());
    }

    @Test
    void createSessionWithHrPersonaShouldReturnHrMode() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setJobRole("产品经理");
        request.setDifficulty(1);
        request.setInterviewMode(InterviewConstants.MODE_BIG_COMPANY_HR);

        InterviewSessionResponse mockResponse = InterviewSessionResponse.builder()
                .sessionId("session-hr")
                .jobRole("产品经理")
                .interviewMode(InterviewConstants.MODE_BIG_COMPANY_HR)
                .interviewModeDesc("大厂 HR 面")
                .build();

        when(interviewService.createSession(eq(1L), eq(request))).thenReturn(mockResponse);

        Result<InterviewSessionResponse> result = controller.createSession(request, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals("session-hr", result.getData().getSessionId());
        assertEquals("大厂 HR 面", result.getData().getInterviewModeDesc());
    }

    @Test
    void sendMessageShouldReturnResponse() {
        String sessionId = "session-1";
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("我负责支付模块");
        request.setFeedbackMode(InterviewConstants.FEEDBACK_MODE_IMMEDIATE);

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .replyContent("请具体说明幂等方案。\n\n<FEEDBACK>\n本题反馈：回答方向清晰，但还需要补充具体处理细节。\n</FEEDBACK>")
                .build();

        when(interviewService.sendMessage(eq(1L), eq(sessionId), eq(request))).thenReturn(mockResponse);

        Result<SendMessageResponse> result = controller.sendMessage(sessionId, request, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertTrue(result.getData().getReplyContent().contains("<FEEDBACK>"));
    }

    @Test
    void getSessionDetailShouldReturnSessionWithPersona() {
        String sessionId = "session-tech";
        InterviewSessionResponse mockResponse = InterviewSessionResponse.builder()
                .sessionId(sessionId)
                .jobRole("Java工程师")
                .interviewMode(InterviewConstants.MODE_TECH_LEADER)
                .interviewModeDesc("技术 Leader 面")
                .build();

        when(interviewService.getSessionDetail(1L, sessionId)).thenReturn(mockResponse);

        Result<InterviewSessionResponse> result = controller.getSessionDetail(sessionId, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals("技术 Leader 面", result.getData().getInterviewModeDesc());
        assertEquals("tech_leader", result.getData().getInterviewMode());
    }

    @Test
    void getSessionDetailWithForeignPersonaShouldReturnForeignMode() {
        String sessionId = "session-foreign";
        InterviewSessionResponse mockResponse = InterviewSessionResponse.builder()
                .sessionId(sessionId)
                .jobRole("后端工程师")
                .interviewMode(InterviewConstants.MODE_FOREIGN_INTERVIEWER)
                .interviewModeDesc("外企面试官")
                .build();

        when(interviewService.getSessionDetail(1L, sessionId)).thenReturn(mockResponse);

        Result<InterviewSessionResponse> result = controller.getSessionDetail(sessionId, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals("外企面试官", result.getData().getInterviewModeDesc());
    }

    @Test
    void endSessionShouldReturnSuccess() {
        String sessionId = "session-1";

        doNothing().when(interviewService).endSession(1L, sessionId);

        Result<Void> result = controller.endSession(sessionId, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        verify(interviewService).endSession(1L, sessionId);
    }

    @Test
    void getHistoryShouldReturnPagedResult() {
        PageResult<InterviewHistoryResponse> pageResult = new PageResult<>();
        pageResult.setList(List.of());
        pageResult.setTotal(0L);
        pageResult.setPageNum(1);
        pageResult.setPageSize(5);

        when(interviewService.getHistory(1L, 1, 5)).thenReturn(pageResult);

        Result<PageResult<InterviewHistoryResponse>> result = controller.getHistory(1, 5, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(Long.valueOf(0), result.getData().getTotal());
    }

    @Test
    void clearHistoryShouldReturnDeletedCount() {
        when(interviewService.clearHistory(1L)).thenReturn(2);

        Result<com.airesume.server.dto.user.DataCleanupResponse> result = controller.clearHistory(authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(2, result.getData().getDeletedCount());
        verify(interviewService).clearHistory(1L);
    }

    @Test
    void streamMessageShouldReturnEmitter() {
        String sessionId = "session-1";
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("请介绍你的项目");

        doNothing().when(aiAsyncExecutor).execute(any());

        ResponseBodyEmitter emitter = controller.streamMessage(sessionId, request, authentication);
        assertNotNull(emitter);
        assertEquals(120_000L, emitter.getTimeout());
    }
}

package com.airesume.server.mq;

import com.airesume.server.service.impl.ResumeDiagnosisProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DirectProcessRouterTest {

    @Test
    void shouldReserveDirectSlotAtomicallyBeforeSubmittingTask() {
        Executor holdingExecutor = command -> {
            // 模拟线程池已接收任务但尚未执行完成，使直连占位保持在 in-flight 状态。
        };
        DirectProcessRouter router = new DirectProcessRouter(holdingExecutor, mock(ResumeDiagnosisProcessor.class));
        ReflectionTestUtils.setField(router, "directThreshold", 1);

        boolean firstSubmitted = router.submitDirectIfCapacity(1L, 10L, "/uploads/resumes/a.pdf");
        boolean secondSubmitted = router.submitDirectIfCapacity(2L, 10L, "/uploads/resumes/b.pdf");

        assertTrue(firstSubmitted);
        assertFalse(secondSubmitted);
    }

    @Test
    void shouldKeepLegacyCheckThenSubmitWithinReservedCapacity() {
        Executor holdingExecutor = command -> {
            // 模拟任务尚未完成，直连槽位保持占用。
        };
        DirectProcessRouter router = new DirectProcessRouter(holdingExecutor, mock(ResumeDiagnosisProcessor.class));
        ReflectionTestUtils.setField(router, "directThreshold", 1);

        assertTrue(router.canProcessDirectly());
        router.submitDirect(1L, 10L, "/uploads/resumes/a.pdf");

        assertFalse(router.canProcessDirectly());
    }
}

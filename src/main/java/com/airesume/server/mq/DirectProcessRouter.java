package com.airesume.server.mq;

import com.airesume.server.service.impl.ResumeDiagnosisProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简历诊断直连路由器。
 *
 * 负载判断逻辑：
 * - 当前正在直连处理的任务数 < 阈值时，任务直接提交到线程池异步处理（不经过 MQ）
 * - 当前正在直连处理的任务数 >= 阈值时，任务回退到 RabbitMQ 队列
 *
 * 这样少量任务时响应更快（无 MQ 开销），大量任务时 MQ 提供可靠排队保障。
 */
@Slf4j
@Component
public class DirectProcessRouter {

    /** 当前正在直连处理的任务数（线程安全） */
    private final AtomicInteger inFlightCount = new AtomicInteger(0);

    /** 直连处理并发阈值，超过此数量的任务走 MQ，可通过 application.yml 配置 */
    @Value("${app.diagnosis.direct-threshold:3}")
    private int directThreshold;

    /** 复用已有的 AI 异步线程池 */
    private final Executor executor;

    /** 共享处理器 */
    private final ResumeDiagnosisProcessor processor;

    public DirectProcessRouter(
            @Qualifier("aiAsyncExecutor") Executor executor,
            ResumeDiagnosisProcessor processor) {
        this.executor = executor;
        this.processor = processor;
    }

    /**
     * 判断当前是否可以走直连异步处理。
     *
     * @return true 表示有空闲槽位可直连处理，false 表示应走 MQ
     */
    public boolean canProcessDirectly() {
        return inFlightCount.get() < directThreshold;
    }

    /**
     * 提交任务到直连异步线程池处理。
     *
     * 调用前应先检查 canProcessDirectly()。
     * 如果线程池拒绝执行，会抛出 RejectedExecutionException，调用方应回退到 MQ。
     *
     * @param taskId  任务ID
     * @param userId  用户ID
     * @param fileUrl 简历文件地址
     */
    public void submitDirect(Long taskId, Long userId, String fileUrl) {
        inFlightCount.incrementAndGet();
        try {
            executor.execute(() -> {
                try {
                    log.info("直连异步处理开始, taskId: {}", taskId);
                    processor.processTask(taskId, userId, fileUrl);
                } finally {
                    inFlightCount.decrementAndGet();
                    log.info("直连异步处理结束, taskId: {}, 当前直连任务数: {}", taskId, inFlightCount.get());
                }
            });
        } catch (Exception e) {
            // 线程池拒绝执行时递减计数器，避免泄漏
            inFlightCount.decrementAndGet();
            throw e;
        }
    }
}

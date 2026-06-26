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
 * 少量任务可直接提交到本地异步线程池，系统繁忙时回退到 RabbitMQ 排队。
 */
@Slf4j
@Component
public class DirectProcessRouter {

    /** 当前正在直连处理的任务数。 */
    private final AtomicInteger inFlightCount = new AtomicInteger(0);
    private final ThreadLocal<Boolean> reservedByCurrentThread = ThreadLocal.withInitial(() -> false);

    /** 直连处理并发阈值，超过该数量的任务应回退 MQ。 */
    @Value("${app.diagnosis.direct-threshold:3}")
    private int directThreshold;

    private final Executor executor;
    private final ResumeDiagnosisProcessor processor;

    public DirectProcessRouter(
            @Qualifier("aiAsyncExecutor") Executor executor,
            ResumeDiagnosisProcessor processor) {
        this.executor = executor;
        this.processor = processor;
    }

    /**
     * 判断当前是否看起来还有直连容量。
     *
     * 该方法只用于监控或兼容旧调用；任务提交应优先使用 submitDirectIfCapacity，避免检查与提交分离导致并发穿透。
     */
    public boolean canProcessDirectly() {
        boolean reserved = reserveDirectSlot();
        reservedByCurrentThread.set(reserved);
        return reserved;
    }

    /**
     * 原子化检查直连容量并提交任务，避免并发请求同时通过阈值检查。
     *
     * @return true 表示已提交到直连线程池，false 表示当前无直连容量，应回退 MQ
     */
    public boolean submitDirectIfCapacity(Long taskId, Long userId, String fileUrl) {
        if (!reserveDirectSlot()) {
            return false;
        }
        try {
            executeReservedTask(taskId, userId, fileUrl);
            return true;
        } catch (Exception e) {
            inFlightCount.decrementAndGet();
            throw e;
        }
    }

    /**
     * 兼容旧调用方的直接提交方法；调用方必须自行保证容量检查。
     */
    public void submitDirect(Long taskId, Long userId, String fileUrl) {
        boolean alreadyReserved = reservedByCurrentThread.get();
        reservedByCurrentThread.remove();
        if (!alreadyReserved) {
            inFlightCount.incrementAndGet();
        }
        try {
            executeReservedTask(taskId, userId, fileUrl);
        } catch (Exception e) {
            inFlightCount.decrementAndGet();
            throw e;
        }
    }

    private boolean reserveDirectSlot() {
        while (true) {
            int current = inFlightCount.get();
            if (current >= directThreshold) {
                return false;
            }
            if (inFlightCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private void executeReservedTask(Long taskId, Long userId, String fileUrl) {
        executor.execute(() -> {
            try {
                log.info("直连异步处理开始, taskId: {}", taskId);
                processor.processTask(taskId, userId, fileUrl);
            } finally {
                inFlightCount.decrementAndGet();
                log.info("直连异步处理结束, taskId: {}, 当前直连任务数: {}", taskId, inFlightCount.get());
            }
        });
    }
}

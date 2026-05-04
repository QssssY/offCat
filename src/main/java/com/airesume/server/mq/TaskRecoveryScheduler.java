package com.airesume.server.mq;

import com.airesume.server.service.ResumeDiagnosisTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度器：回收因消费者崩溃、服务重启等原因卡在处理中状态的孤儿任务。
 *
 * 每5分钟扫描一次，将超过10分钟仍处于PROCESSING状态的任务标记为FAILED。
 * 这样即使消息已被消费确认（ACK），任务状态也不会永久卡死。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskRecoveryScheduler {

    /** 孤儿任务超时阈值（分钟）：超过此时间仍为PROCESSING的任务将被回收 */
    private static final int ORPHAN_TIMEOUT_MINUTES = 10;

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;

    /**
     * 每5分钟执行一次，回收超时的处理中任务。
     * initialDelay=60000 表示应用启动后延迟1分钟再执行首次扫描，避免启动阶段误回收。
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void recoverOrphanedTasks() {
        try {
            int recovered = resumeDiagnosisTaskService.recoverOrphanedTasks(ORPHAN_TIMEOUT_MINUTES);
            if (recovered > 0) {
                log.info("定时回收孤儿任务完成, 回收数量: {}", recovered);
            }
        } catch (Exception e) {
            log.error("定时回收孤儿任务异常", e);
        }
    }
}

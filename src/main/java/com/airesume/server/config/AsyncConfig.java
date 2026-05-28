package com.airesume.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 * 用于 AI 生成（开场白、评估报告）等耗时异步任务，避免占用 ForkJoinPool.commonPool。
 */
@Configuration
public class AsyncConfig {

    /**
     * AI 异步任务专用线程池。
     * 核心 2 线程、最大 8 线程、队列容量 50，拒绝策略为 CallerRuns（队列满时由调用线程执行，避免丢失任务）。
     */
    @Bean("aiAsyncExecutor")
    public Executor aiAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 管理端看板并行查询专用线程池。
     * 看板需要同时发起多个 COUNT 查询，并行化可显著降低响应延迟。
     * 核心 4 线程即可覆盖所有看板并行场景，队列满时由调用线程降级串行执行。
     */
    @Bean("dashboardExecutor")
    public Executor dashboardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("dashboard-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

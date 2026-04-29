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
}

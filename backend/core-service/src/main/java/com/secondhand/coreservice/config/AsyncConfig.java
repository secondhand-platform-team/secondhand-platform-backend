package com.secondhand.coreservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Cấu hình Thread Pool cho các tác vụ @Async.
 *
 * Dùng cho:
 *   - ItemExpiryScheduler.sendExpiryNotificationsAsync()
 *   - Các tác vụ nền không block luồng chính
 *
 * Cấu hình Thread Pool:
 *   corePoolSize  = 2   → số thread luôn sẵn sàng
 *   maxPoolSize   = 5   → tối đa khi queue đầy
 *   queueCapacity = 100 → số tác vụ chờ trong queue
 *
 * Điều chỉnh theo tải thực tế của hệ thống.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

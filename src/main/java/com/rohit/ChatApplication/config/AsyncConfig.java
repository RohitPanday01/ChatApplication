package com.rohit.ChatApplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


import java.util.concurrent.*;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("presenceTaskExecutor")
    public ThreadPoolTaskExecutor executor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // minimum number of threads
        executor.setMaxPoolSize(10);        // maximum number of threads
        executor.setQueueCapacity(1000);    // queue before rejecting
        executor.setThreadNamePrefix("Notification-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "rateLimiterTaskExecutor")
    public Executor rateLimiterTaskConfig(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("RateLimiter-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;


    }

    @Bean
    public ThreadPoolExecutor groupWorkerPool() {
        return new ThreadPoolExecutor(
                6,                          // core threads
                10,                         // max threads
                60,                          // idle thread keep-alive (seconds)
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(10000), // large queue
                new ThreadPoolExecutor.CallerRunsPolicy() // fallback if saturated
        );
    }

}

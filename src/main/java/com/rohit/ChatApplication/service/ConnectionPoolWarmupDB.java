package com.rohit.ChatApplication.service;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Component
public class ConnectionPoolWarmupDB implements ApplicationListener<ApplicationReadyEvent> {
    private final DataSource dataSource;
    
    public ConnectionPoolWarmupDB(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        int targetWarmup = resolveMinimumIdle();
        if (targetWarmup <= 0) return;

        log.info("Warming up {} DB connections in parallel...", targetWarmup);

        ExecutorService warmupExecutor = Executors.newFixedThreadPool(targetWarmup);
        CompletableFuture<?>[] futures = new CompletableFuture[targetWarmup];

        for (int i = 0; i < targetWarmup; i++) {
            futures[i] = CompletableFuture.runAsync(this::warmupSingleConnection, warmupExecutor);
        }

        // Wait for all connections to execute SELECT 1 before opening app to traffic
        CompletableFuture.allOf(futures).join();
        warmupExecutor.shutdown();

        log.info("Connection pool warmup complete across {} connections.", targetWarmup);

    }

    private void warmupSingleConnection() {
        // Safe try-with-resources auto-closes Connection, Statement, and ResultSet
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int dummy = rs.getInt(1); // Touches data to exercise full driver pipeline
            }
        } catch (SQLException e) {
            log.warn("Warmup connection query failed: {}", e.getMessage());
        }
    }

    private int resolveMinimumIdle() {
        try {
            // Safely unwrap HikariDataSource even if wrapped in Spring proxies
            if (dataSource.isWrapperFor(HikariDataSource.class)) {
                return dataSource.unwrap(HikariDataSource.class).getMinimumIdle();
            } else if (dataSource instanceof HikariDataSource hikari) {
                return hikari.getMinimumIdle();
            }
        } catch (SQLException e) {
            log.warn("Could not unwrap HikariDataSource for warmup count", e);
        }
        return 5; // Default fallback
    }
}

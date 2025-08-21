package com.rohit.ChatApplication.Batch.LastSeen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LastSeenFlushScheduler {

    private final JobLauncher jobLauncher;
    private final Job updateLastSeenJob;

    @Scheduled(fixedDelay = 90000)
    public void runLastSeenFlushJob() {
        JobParameters jobParams = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis()) // makes it unique
                .toJobParameters();

        try {
            jobLauncher.run(updateLastSeenJob, jobParams);
        } catch (Exception e) {
            log.error("Failed to flush last seen job", e);
        }
    }

}

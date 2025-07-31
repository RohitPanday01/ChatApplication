package com.rohit.ChatApplication.Batch.LastSeen;

import com.rohit.ChatApplication.data.UserLastSeen;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class LastSeenBatchJobConfig {


    @Bean
    public Job updateLastSeenJob(JobRepository jobRepository , Step updateLastSeenStep ){
        return new JobBuilder("updateLastSeenJob", jobRepository)
                .start(updateLastSeenStep)
                .build();
    }

    @Bean
    public Step updateLastSeenStep(JobRepository jobRepository,
                                   LastSeenWriter lastSeenWriter,RedisLastSeenReader redisLastSeenReader,
                                   PlatformTransactionManager transactionManager ){
        return new StepBuilder("updateLastSeenStep", jobRepository)
                .<UserLastSeen, UserLastSeen>chunk(100, transactionManager)
                .reader(redisLastSeenReader)
                .writer(lastSeenWriter)
                .build();

    }


}

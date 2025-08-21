package com.rohit.ChatApplication;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories("com.rohit.ChatApplication.repository")
@EntityScan("com.rohit.ChatApplication.entity")
@EnableBatchProcessing
@EnableScheduling
public class ChatApplication {

	public static void main(String[] args) {

		 SpringApplication.run(ChatApplication.class, args);

	}

}

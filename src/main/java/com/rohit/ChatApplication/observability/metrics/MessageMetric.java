package com.rohit.ChatApplication.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class MessageMetric {
    private final Counter messagesPublished;
    private final Counter publishFailures;

    public MessageMetric(MeterRegistry meterRegistry){
        this.messagesPublished = Counter.builder("chat.messages.published.total")
                .description("Total chat messages accepted by publish API")
                .register(meterRegistry);

        this.publishFailures = Counter.builder("chat.messages.publish.failures.total")
                .description("Total chat message publish failures")
                .register(meterRegistry);
    }

    public void incrementMessagesPublished() {
        messagesPublished.increment();
    }

    public void incrementPublishFailures() {
        publishFailures.increment();
    }
}

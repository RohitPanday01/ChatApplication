package com.rohit.ChatApplication.observability.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

@Component
public class ChatMetrics {

    private final AtomicInteger activeSessions = new AtomicInteger(0);

    private final Counter connectionsOpened ;
    private final Counter connectionsClosed;
    private final Counter sendFailures;
    private final Counter wsMessagesDelivered;
    private final Timer messageProcessingTimer;
    private final Timer e2eDeliveryTimer;


    public ChatMetrics(MeterRegistry meterRegistry) {

        // 1. Gauge: Reflects the instantaneous active socket count
        Gauge.builder("ws.sessions.active", activeSessions, AtomicInteger::get)
                .description("Number of currently active raw WebSocket sessions")
                .register(meterRegistry);

        // 2. Counters: For tracking cumulative churn
        this.connectionsOpened = Counter.builder("ws.connections.opened")
                .description("Total raw WebSocket connections opened")
                .register(meterRegistry);

        this.connectionsClosed = Counter.builder("ws.connections.closed")
                .description("Total raw WebSocket connections closed")
                .register(meterRegistry);


        this.wsMessagesDelivered =
                Counter.builder("ws.messages.delivered.total")
                        .register(meterRegistry);

        this.sendFailures = Counter.builder("ws.send.failures")
                .description("Total raw WebSocket session send failures")
                .register(meterRegistry);

        // 3. Timers: Configured with percentiles for load-testing analysis
        this.messageProcessingTimer = Timer.builder("ws.message.processing.time")
                .description("Time spent processing an incoming raw WebSocket message")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        this.e2eDeliveryTimer = Timer.builder("ws.message.delivery.time")
                .description("End-to-end message latency: Ingress -> Kafka -> Consumer -> Egress")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void incrementActiveSessions() { activeSessions.incrementAndGet(); }
    public void decrementActiveSessions() { activeSessions.decrementAndGet(); }
    public void incrementConnectionsOpened() { connectionsOpened.increment(); }
    public void incrementConnectionsClosed() { connectionsClosed.increment(); }
    public void incrementSendFailures() { sendFailures.increment(); }
    public void incrementDelivered() {
        wsMessagesDelivered.increment();
    }


    public Timer getMessageProcessingTimer() { return messageProcessingTimer; }

    public void recordE2EDelivery(long startTimeNanos) {
        e2eDeliveryTimer.record(Instant.now().toEpochMilli() - startTimeNanos, TimeUnit.MILLISECONDS);
    }


}

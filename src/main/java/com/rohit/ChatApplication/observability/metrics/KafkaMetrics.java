package com.rohit.ChatApplication.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class KafkaMetrics {
    private final Counter kafkaDmProduced;
    private final Counter kafkaForwardProduced;
    private final Counter kafkaReadReceiptProduced;

    @Getter
    private final Timer kafkaConsumerProcessing;
    private final Counter consumerSuccess;
    private final Counter consumerFailure;
    private final Counter retryCounterKafkaConsumer;


    public KafkaMetrics(MeterRegistry meterRegistry){
        this.kafkaDmProduced = Counter.builder("kafka.dm.produced.total")
                .register(meterRegistry);
        this.kafkaForwardProduced = Counter.builder("kafka.forward.produced.total")
                .register(meterRegistry);
        this.kafkaReadReceiptProduced = Counter.builder("kafka.readReceipt.produced.total")
                .register(meterRegistry);
        this.consumerSuccess = Counter.builder("kafka.consumer.success")
                .register(meterRegistry);
        this.consumerFailure = Counter.builder("kafka.consumer.failure")
                .register(meterRegistry);
        this.retryCounterKafkaConsumer = Counter.builder("kafka.consumer.retry")
                .register(meterRegistry);
        this.kafkaConsumerProcessing = Timer.builder("kafka.consumer.processing.time")
                .publishPercentileHistogram()
                .publishPercentiles(0.5,0.9,0.95,0.99)
                .register(meterRegistry);
    }

    public void incrementDmProduced() {
        kafkaDmProduced.increment();
    }

    public void incrementForwardProduced() {
        kafkaForwardProduced.increment();
    }

    public void incrementReadReceiptProduced() {
        kafkaReadReceiptProduced.increment();
    }

    public void incrementConsumerSuccess(){
        consumerSuccess.increment();
    }

    public void incrementConsumerFailure(){
        consumerFailure.increment();
    }

    public void incrementRetryCounterKafkaConsumer(){
        retryCounterKafkaConsumer.increment();
    }
}

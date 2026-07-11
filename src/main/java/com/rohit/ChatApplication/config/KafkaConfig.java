package com.rohit.ChatApplication.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.NotificationEvent;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.message.GroupMessageDto;

import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG ,120000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG , 50);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return props;
    }

    @Bean
    public ProducerFactory<String, Object> transactionalProducerFactory(){
        DefaultKafkaProducerFactory<String, Object> factory =
                new DefaultKafkaProducerFactory<>(producerConfigs());
        factory.setTransactionIdPrefix("chat-tx-");
        return factory;
    }

    @Bean(name = "transactionalKafkaTemplate")
    public KafkaTemplate<String, Object> transactionalKafkaTemplate() {
        return new KafkaTemplate<>(transactionalProducerFactory());
    }

    @Bean
    public  ProducerFactory<String, Object> producerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }


    @Bean
    public KafkaTemplate<String , Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public <T> ConsumerFactory<String, T> persistConsumerFactory(Class<T> targetType, String functionalGroupId) {
        Map<String, Object > props = new HashMap<>();
        String hostIp = generateUniqueInstanceId();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,  300000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 15000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG , "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, functionalGroupId );
        // GROUP_INSTANCE_ID_CONFIG use it as a static instance ID
        // to prevent rebalances when restarting the monolith container
        props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, functionalGroupId + "-" + hostIp);


        JsonDeserializer<T> deserializer =
                new JsonDeserializer<>(targetType);
        deserializer.ignoreTypeHeaders();  // always bind to PrivateMessageDto
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                deserializer);
    }

    private String generateUniqueInstanceId() {
        try {
            // Extracts the network name of your EC2 instance (e.g., ip-10-0-1-45)
            return InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            // Fallback to a random unique identifier if network resolution fails
            return "chat-app-" + UUID.randomUUID();
        }
    }


    public <T> ConsumerFactory<String, T> consumerFactory(Class<T> targetType, String functionalGroupId){
        String hostIp = generateUniqueInstanceId();

        Map<String, Object > props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 15000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG , "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, functionalGroupId );
        // GROUP_INSTANCE_ID_CONFIG use it as a static instance ID
        // to prevent rebalances when restarting the monolith container
        props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, functionalGroupId + "-" + hostIp);

        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetType);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean(name = "persistContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String , PrivateMessageDto> persistFactory(DefaultErrorHandler errorHandler){
        ConcurrentKafkaListenerContainerFactory<String, PrivateMessageDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(persistConsumerFactory(PrivateMessageDto.class , "dm-persistence-cg"));
        factory.setCommonErrorHandler(errorHandler);
        factory.setBatchListener(true);
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        // Disable virtual threads by using the default platform thread executor
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-container-persist");
        executor.setVirtualThreads(false); // This ensures platform threads are used

        factory.getContainerProperties().setListenerTaskExecutor(executor);

        return factory;
    }

    @Bean(name = "deliveryContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PrivateMessageDto > deliveryFactory(DefaultErrorHandler errorHandler){
        ConcurrentKafkaListenerContainerFactory<String, PrivateMessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory(PrivateMessageDto.class, "private-message-cg" ));
        factory.setConcurrency(2);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        // Disable virtual threads by using the default platform thread executor
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-container-dmDelivery");
        executor.setVirtualThreads(false); // This ensures platform threads are used

        factory.getContainerProperties().setListenerTaskExecutor(executor);
        return factory;
    }


    @Bean(name =  "GroupMessageDeliveryContainer")
    public ConcurrentKafkaListenerContainerFactory<String ,
            GroupMessageDto > GroupMessageDeliveryFactory(DefaultErrorHandler errorHandler){

        ConcurrentKafkaListenerContainerFactory<String , GroupMessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(GroupMessageDto.class , "group-message-cg"));
        factory.setConcurrency(2);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        // Disable virtual threads by using the default platform thread executor
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-container-groupMessage");
        executor.setVirtualThreads(false); // This ensures platform threads are used

        factory.getContainerProperties().setListenerTaskExecutor(executor);
        return factory;

    }

    @Bean(name = "notificationContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String , NotificationEvent> notificationFactory(DefaultErrorHandler errorHandler){
        ConcurrentKafkaListenerContainerFactory<String ,NotificationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(NotificationEvent.class ,"notification-cg" ));
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        // Disable virtual threads by using the default platform thread executor
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-container-notification");
        executor.setVirtualThreads(false); // This ensures platform threads are used

        factory.getContainerProperties().setListenerTaskExecutor(executor);
        return factory;
    }

    @Bean(name = "readReceiptContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String , ReadReceipt> readReceiptFactory(DefaultErrorHandler errorHandler){
        ConcurrentKafkaListenerContainerFactory<String ,ReadReceipt> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(ReadReceipt.class ,"readReceipt-cg"));
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        // Disable virtual threads by using the default platform thread executor
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-container-readReciept");
        executor.setVirtualThreads(false); // This ensures platform threads are used

        factory.getContainerProperties().setListenerTaskExecutor(executor);
        return factory;
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String ,Object> kafkaTemplate){
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (consumerRecord, exception) ->
                                new TopicPartition(consumerRecord.topic()+"-DLT", consumerRecord.partition()));


        FixedBackOff backOff = new FixedBackOff(2000L, 5);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer , backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }


}

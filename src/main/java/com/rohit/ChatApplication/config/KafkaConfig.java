package com.rohit.ChatApplication.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public  ProducerFactory<String, PrivateMessageDto> producerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,"my-tx-producer-1");
        return props;
    }

    @Bean
    public KafkaTemplate<String , PrivateMessageDto> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public ConsumerFactory<String, PrivateMessageDto> persistConsumerFactory() {
        Map<String, Object > props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dm-persistence-svc");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG , "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<PrivateMessageDto> deserializer = new JsonDeserializer<>(
                PrivateMessageDto.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConsumerFactory<String, PrivateMessageDto> deliveryConsumerFactory() {
        Map<String, Object > props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dm-delivery-svc");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG , "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<PrivateMessageDto> deserializer = new JsonDeserializer<>(PrivateMessageDto.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean(name = "persistContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String , PrivateMessageDto> persistFactory(){
        ConcurrentKafkaListenerContainerFactory<String, PrivateMessageDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(persistConsumerFactory());
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean(name = "deliveryContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PrivateMessageDto > deliveryFactory(){
        ConcurrentKafkaListenerContainerFactory<String, PrivateMessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(deliveryConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }


}

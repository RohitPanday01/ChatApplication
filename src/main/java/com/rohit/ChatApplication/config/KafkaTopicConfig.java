package com.rohit.ChatApplication.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic readReceiptTopic(){
        return  new NewTopic("read-receipt",3 , (short) 1 );
    }

    @Bean
    public NewTopic interNodeReadReceiptTopic(){
        return  new NewTopic("inter-node-read-receipt",3 , (short) 1 );
    }

    @Bean
    public NewTopic dmNotifyTopic (){
        return  new NewTopic("dm-notify",3 , (short) 1 );
    }
    @Bean
    public NewTopic groupMessageInterNodeTopic() {
        return new NewTopic("groupMessage-inter-node", 3, (short) 1);
    }

    @Bean
    public NewTopic groupDeliveryTopic() {
        return new NewTopic("group-delivery", 3, (short) 1);
    }

    @Bean
    public NewTopic interNodeDmDeliveryTopic() {
        return new NewTopic("inter-node-dm-delivery", 3, (short) 1);
    }

    @Bean
    public NewTopic dmDeliveryTopic() {
        return new NewTopic("dm-delivery", 3, (short) 1);
    }

    @Bean
    public NewTopic dmPersistTopic() {
        return new NewTopic("dm-persist", 3, (short) 1);
    }
}

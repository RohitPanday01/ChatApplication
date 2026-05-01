package com.rohit.ChatApplication.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${chat.topics.read-receipt}")
    private String readReceipt;

    @Value("${chat.topics.inter-node-read-receipt}")
    private String interNodeReadReceipt;

    @Value("${chat.topics.dm-notify}")
    private String dmNotify;

    @Value("${chat.topics.groupMessage-inter-node}")
    private String groupMessageInterNode;

    @Value("${chat.topics.group-delivery}")
    private String groupDelivery;

    @Value("${chat.topics.inter-node-dm-delivery}")
    private String interNodeDmDelivery;

    @Value("${chat.topics.dm-delivery}")
    private String dmDelivery;

    @Value("${chat.topics.dm-persist}")
    private String dmPersist;

    @Bean
    public NewTopic readReceiptTopic(){
        return  new NewTopic(readReceipt,3 , (short) 1 );
    }

    @Bean
    public NewTopic interNodeReadReceiptTopic(){
        return  new NewTopic(interNodeReadReceipt,3 , (short) 1 );
    }

    @Bean
    public NewTopic dmNotifyTopic (){
        return  new NewTopic(dmNotify,3 , (short) 1 );
    }
    @Bean
    public NewTopic groupMessageInterNodeTopic() {
        return new NewTopic(groupMessageInterNode, 3, (short) 1);
    }

    @Bean
    public NewTopic groupDeliveryTopic() {
        return new NewTopic(groupDelivery, 3, (short) 1);
    }

    @Bean
    public NewTopic interNodeDmDeliveryTopic() {
        return new NewTopic(interNodeDmDelivery, 3, (short) 1);
    }

    @Bean
    public NewTopic dmDeliveryTopic() {
        return new NewTopic(dmDelivery, 3, (short) 1);
    }

    @Bean
    public NewTopic dmPersistTopic() {
        return new NewTopic(dmPersist, 3, (short) 1);
    }
}

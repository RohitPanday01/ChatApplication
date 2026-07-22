package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.message.NodeIdentity;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NodeChannelInitializer {
    @Autowired
    private NodeIdentity nodeIdentity;

    @Autowired
    private SessionSubscriptionManager subscriptionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        String thisServerNodeId = nodeIdentity.getNodeId();
        subscriptionManager.subscribeUserTypingChannel(thisServerNodeId);

        LoggerFactory.getLogger(NodeChannelInitializer.class)
                .info("Permanently subscribed node typing channel: {}", thisServerNodeId);
    }
}

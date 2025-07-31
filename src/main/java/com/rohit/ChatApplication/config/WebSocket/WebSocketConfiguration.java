package com.rohit.ChatApplication.config.WebSocket;

import com.rohit.ChatApplication.interceptor.CsrfChannelInterceptor;
import com.rohit.ChatApplication.interceptor.JwtCookieHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private TaskScheduler messageBrokerTaskScheduler;

    private final JwtCookieHandshakeInterceptor jwtCookieHandshakeInterceptor;

    public WebSocketConfiguration (JwtCookieHandshakeInterceptor jwtCookieHandshakeInterceptor){
        this.jwtCookieHandshakeInterceptor  = jwtCookieHandshakeInterceptor;
    }

    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler messageBrokerTaskScheduler) {
        this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config){
       config.enableSimpleBroker("/topic" , "/queue")
               .setHeartbeatValue(new long[]{10000 ,20000})
               .setTaskScheduler(this.messageBrokerTaskScheduler);
       config.setApplicationDestinationPrefixes("/app");
       config.setUserDestinationPrefix("/user");
    }

    public void  registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .addInterceptors(jwtCookieHandshakeInterceptor );
    }
 
    @Bean
    public TaskScheduler heartBeatScheduler(){
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-thread");
        return scheduler;
    }

//    @Bean
//    public ServletServerContainerFactoryBean createWebSocketContainer() {
//        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
//        container.setMaxTextMessageBufferSize(64 * 1024);
//        container.setMaxSessionIdleTimeout(10 * 60 * 1000L);
//        return container;
//    }

}

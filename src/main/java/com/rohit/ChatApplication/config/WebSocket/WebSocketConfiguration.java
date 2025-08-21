package com.rohit.ChatApplication.config.WebSocket;

import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
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
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final JwtCookieHandshakeInterceptor jwtCookieHandshakeInterceptor ;
    private final PresenceWSHandler presenceWSHandler;

    public WebSocketConfiguration(JwtCookieHandshakeInterceptor jwtCookieHandshakeInterceptor,
                                  PresenceWSHandler presenceWSHandler ){
        this.jwtCookieHandshakeInterceptor = jwtCookieHandshakeInterceptor;
        this.presenceWSHandler = presenceWSHandler;

    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry
                .addHandler(presenceWSHandler ,"/ws/channel/presence/subscribe")
                .addInterceptors(jwtCookieHandshakeInterceptor)
                .setAllowedOrigins("*");
    }


}

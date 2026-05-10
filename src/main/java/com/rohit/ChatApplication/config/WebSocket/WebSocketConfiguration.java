package com.rohit.ChatApplication.config.WebSocket;

import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.interceptor.JwtCookieHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;


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
                .addHandler(presenceWSHandler ,"/ws/chat")
                .addInterceptors(jwtCookieHandshakeInterceptor)
                .setAllowedOrigins("*");
    }


}

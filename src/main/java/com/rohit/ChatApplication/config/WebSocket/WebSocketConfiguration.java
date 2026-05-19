package com.rohit.ChatApplication.config.WebSocket;

import com.rohit.ChatApplication.controller.Websocket.MetricWebSocketHandlerDecorator;
import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.interceptor.JwtCookieHandshakeInterceptor;
import com.rohit.ChatApplication.observability.metrics.ChatMetrics;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final JwtCookieHandshakeInterceptor jwtCookieHandshakeInterceptor ;
    private final PresenceWSHandler presenceWSHandler;
    private final ChatMetrics chatMetrics;


    public WebSocketConfiguration(JwtCookieHandshakeInterceptor jwtCookieHandshakeInterceptor,
                                  PresenceWSHandler presenceWSHandler, ChatMetrics chatMetrics ){
        this.jwtCookieHandshakeInterceptor = jwtCookieHandshakeInterceptor;
        this.presenceWSHandler = presenceWSHandler;
        this.chatMetrics = chatMetrics;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry
                .addHandler(new MetricWebSocketHandlerDecorator(presenceWSHandler , chatMetrics),"/ws/chat")
                .addInterceptors(jwtCookieHandshakeInterceptor)
                .setAllowedOrigins("*");
    }


}

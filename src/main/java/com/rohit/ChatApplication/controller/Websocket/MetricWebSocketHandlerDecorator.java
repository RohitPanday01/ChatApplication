package com.rohit.ChatApplication.controller.Websocket;

import com.rohit.ChatApplication.observability.metrics.ChatMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class MetricWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

    private final ChatMetrics metrics;

    public MetricWebSocketHandlerDecorator(WebSocketHandler delegate , ChatMetrics metrics) {
        super(delegate);
        this.metrics = metrics;
    }

    @Override
    public void afterConnectionEstablished(@NonNull  WebSocketSession session) throws Exception {
        metrics.incrementConnectionsOpened();
        metrics.incrementActiveSessions();
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull  WebSocketSession session, @NonNull CloseStatus closeStatus) throws Exception {
        metrics.decrementActiveSessions();
        metrics.incrementConnectionsClosed();
        super.afterConnectionClosed(session, closeStatus);
    }

    @Override
    public void handleMessage(@NonNull  WebSocketSession session, @NonNull  WebSocketMessage<?> message) throws Exception {
        long start = Instant.now().toEpochMilli();
        try {
            super.handleMessage(session, message);
        } catch (Exception e) {
            metrics.incrementSendFailures();
            throw e;
        } finally {
            metrics.getMessageProcessingTimer().record(Instant.now().toEpochMilli() - start, TimeUnit.MILLISECONDS );
        }
    }


}

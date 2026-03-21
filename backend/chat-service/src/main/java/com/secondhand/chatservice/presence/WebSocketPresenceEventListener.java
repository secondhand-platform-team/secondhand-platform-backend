package com.secondhand.chatservice.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {

    private final OnlinePresenceService onlinePresenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getFirstNativeHeader("userId");
        String sessionId = accessor.getSessionId();

        onlinePresenceService.registerSession(userId, sessionId)
                .ifPresent(change -> messagingTemplate.convertAndSend(
                        "/topic/presence",
                        Map.of(
                                "userId", change.userId(),
                                "isOnline", change.isOnline()
                        )
                ));
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        onlinePresenceService.unregisterSession(sessionId)
                .ifPresent(change -> messagingTemplate.convertAndSend(
                        "/topic/presence",
                        Map.of(
                                "userId", change.userId(),
                                "isOnline", change.isOnline()
                        )
                ));
    }
}

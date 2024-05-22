package com.chat.websocket.config;

import com.chat.websocket.model.ChatMessage;
import com.chat.websocket.model.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");

        if (username != null) {
            var chatMesssage = ChatMessage.builder()
                    .type(MessageType.LEAVE)
                    .sender(username).build();

            System.out.println("Disconnected from " + username);
            messagingTemplate.convertAndSend("/topic/public", chatMesssage);
            messagingTemplate.convertAndSend("/topic/userDisconnected", userId);
        }
        if (headerAccessor.getUser() == null || headerAccessor.getSessionAttributes().isEmpty()) {
            headerAccessor.getSessionAttributes().clear();
            System.out.println("Clear Session");
        }
    }


}

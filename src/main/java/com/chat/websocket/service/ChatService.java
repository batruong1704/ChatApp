package com.chat.websocket.service;

import com.chat.websocket.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendPublicMessage(ChatMessage chatMessage) {
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    public void sendPrivateMessage(String target, ChatMessage chatMessage) {
        messagingTemplate.convertAndSendToUser(target, "/queue/messages", chatMessage);
    }

}

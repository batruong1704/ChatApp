package com.chat.websocket.controller;

import com.chat.websocket.model.ChatMessage;
import com.chat.websocket.model.MessageType;
import com.chat.websocket.model.Room;
import com.chat.websocket.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Controller
@RestController
public class ChatController {

    private Map<String, String> users = new HashMap<>();
    private final ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        if (chatMessage.getMessage() == null || chatMessage.getMessage().isEmpty()) {
            chatMessage.setMessage("No content");
        }
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String userId = UUID.randomUUID().toString();
        users.put(userId, username);
        headerAccessor.getSessionAttributes().put("userId", userId);
        headerAccessor.getSessionAttributes().put("username", username);
        System.out.println("=> New User Join: " + userId + " | " + username);
        ChatMessage welcomeMessage = new ChatMessage();
        welcomeMessage.setType(MessageType.JOIN);
        welcomeMessage.setSender(username);
        welcomeMessage.setMessage("has joined");
        messagingTemplate.convertAndSend("/topic/users", users);
        return welcomeMessage;
    }


    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userId != null) {
            String username = users.get(userId);
            if (username != null) {
                users.remove(userId);
                System.out.println("In Controller: Disconnected from " + userId + " | " + username);
                messagingTemplate.convertAndSend("/topic/users", users);
            }
        }
    }

    @MessageMapping("/chat.getUsers")
    @SendTo("/topic/users")
    public Map<String, String> getUsers() {
        return users;
    }

    private Map<String, Room> rooms = new HashMap<>();
    private boolean lobbyCreated = false;

    @MessageMapping("/chat.createRoom")
    @SendTo("/topic/roomCreated")
    public Room createRoom(@Payload Room roomData, SimpMessageHeaderAccessor headerAccessor) {
        if ("lobby".equals(roomData.getId())) {
            synchronized (this) {
                if (!lobbyCreated) {
                    roomData.setId("lobby");
                    roomData.setName("Lobby");
                    rooms.put("lobby", roomData);
                    lobbyCreated = true;
                    System.out.println("Lobby room created");
                } else {
                    return rooms.get("lobby");
                }
            }
            return roomData;
        }

        roomData.setId(UUID.randomUUID().toString());
        rooms.put(roomData.getId(), roomData);
        System.out.println("Room created: " + roomData);

        return roomData;
    }




}

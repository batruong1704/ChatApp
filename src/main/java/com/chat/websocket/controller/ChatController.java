package com.chat.websocket.controller;

import com.chat.websocket.model.ChatMessage;
import com.chat.websocket.model.MessageType;
import com.chat.websocket.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class ChatController {

    private Map<String, String> users = new HashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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

    public void removeUser(String myId){
        Set<String> keySet = users.keySet();
        if (myId != null) {
            for (String key : keySet) {
                if (myId.equals(key)){
                    users.remove(myId);
                    System.out.println("In Controller: Disconnected from " + myId + " | ");
                    messagingTemplate.convertAndSend("/topic/users", users);
                    return;
                }
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

package juego.arsw.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import juego.arsw.model.User;
import juego.arsw.service.UserService;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private UserService userService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sendUsersList();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User playerData = objectMapper.readValue(message.getPayload(), User.class);
        User processedUser = userService.processUser(playerData);
        
        // Broadcast updated state to all connected clients
        TextMessage response = new TextMessage(objectMapper.writeValueAsString(userService.getAllUsers()));
        for (WebSocketSession clientSession : sessions.values()) {
            if (clientSession.isOpen()) {
                clientSession.sendMessage(response);
            }
        }

        // Store the user in the map
        if (users.containsKey(sessions.get(session.getId()).getId())) {
            users.replace(sessions.get(session.getId()).getId(), processedUser);
        } else {
            users.put(sessions.get(session.getId()).getId(), processedUser);
        }

        sendUsersList();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session.getId());
        users.remove(session.getId());
        sendUsersList(); // Notify all clients about the updated user list
    }

        private void sendUsersList() {
        try {
            List<User> usersList = new ArrayList<>(users.values());
            
            // Crear un objeto que contenga la lista de usuarios
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("type", "usersList");
            responseMap.put("users", usersList);
            
            // Convertir explícitamente a JSON usando ObjectMapper
            String jsonResponse = objectMapper.writeValueAsString(responseMap);
            TextMessage response = new TextMessage(jsonResponse);
            
            for (WebSocketSession clientSession : sessions.values()) {
                if (clientSession.isOpen()) {
                    clientSession.sendMessage(response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
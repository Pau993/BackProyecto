package juego.arsw.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import juego.arsw.model.EntityPerson;
import juego.arsw.model.User;

@RestController
public class UserRestController extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, User> players = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(UserRestController.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, EntityPerson> availablePersons = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRoles = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();

    // Initialize a default EntityPerson object

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String playerId = generatePlate(); // Usar la placa en lugar del sessionId

        // Guardar la sesión con la nueva placa como ID
        sessions.put(playerId, session);

        // También necesitamos mantener un mapeo entre sessionId y playerId
        sessionToPlayerId.put(sessionId, playerId);

        String message = "{\"type\":\"PLAYER_ID\",\"playerId\":\"" + playerId + "\"}";
        session.sendMessage(new TextMessage(message));

        if (availablePersons.isEmpty()) {
            initializeAvailablePersons();
        }

        broadcastAvailablePersons();
        broadcastPlayerStates();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String playerId = sessionToPlayerId.get(sessionId);
        String payload = message.getPayload();

        try {
            JSONObject data = new JSONObject(payload);

            String role = data.optString("role", "user");
            sessionRoles.put(playerId, role);

            if ("admin".equalsIgnoreCase(role)) {
                logger.info("Admin connected: " + playerId);
                sessions.put(playerId, session);

                sendPlayersCountToAdmin(session);
                return;
            }

            sessions.put(playerId, session);

            if (!players.containsKey(playerId)) {
                User newPlayer = new User(playerId);
                String name = data.optString("name", "Player_" + playerId);
                newPlayer.setName(name);
                newPlayer.setHasPerson("0");
                players.put(playerId, newPlayer);
                broadcastPlayersCountToAdmins(); // Notificar a admins
            }

            if (data.has("personId") && data.has("active")) {
                System.out.println(
                        "-------------------------------------------------------------------------------------------------------------------------------------------------------------------");
                handlePersonStateUpdate(data);
                return;

            }

            if (data.has("type") && data.getString("type").equals("collectPerson")) {
                String personId = data.getString("personId");
                handlePersonCollected(playerId, personId);
                return;
            }

            if ((data.has("playerId") || data.has("id")) && data.has("hasPerson")) {
                logger.info("PLAYER UPDATE WORKING: " + payload);
                handlePersonUpdate(payload);
                return;
            }
            updatePlayer(playerId, data);
            broadcastPlayerPositions();
        } catch (Exception e) {
            logger.severe("Error processing message: " + e.getMessage());
            sendErrorMessage(session, "Error processing message");
        }
    }

    private void handlePersonStateUpdate(JSONObject data) {
        try {
            String personId = data.getString("personId");
            boolean active = data.getBoolean("active");

            EntityPerson person = availablePersons.get(personId);
            if (person != null) {
                // Remover la persona si está inactiva
                if (!active) {
                    availablePersons.remove(personId);
                }

                // Broadcast el cambio de estado
                broadcastPersonStateUpdate(personId, active);

                // Broadcast la lista actualizada de personas disponibles
                broadcastAvailablePersons();

                logger.info("Updated person state: " + personId + ", active: " + active);
            } else {
                logger.warning("Person not found: " + personId);
            }
        } catch (Exception e) {
            logger.severe("Error handling person state update: " + e.getMessage());
        }
    }

    private void broadcastPersonStateUpdate(String personId, boolean active) {
        try {
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("type", "personStateUpdate");
            broadcast.put("personId", personId);
            broadcast.put("active", active);

            String jsonMessage = objectMapper.writeValueAsString(broadcast);
            TextMessage message = new TextMessage(jsonMessage);

            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                        logger.info("Broadcast person state: " + jsonMessage);
                    }
                } catch (IOException e) {
                    logger.warning("Error broadcasting to session " + session.getId() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.severe("Error broadcasting person state: " + e.getMessage());
        }
    }

    private void initializeAvailablePersons() {
        availablePersons.put("p1", new EntityPerson("p1", 4, 6, "PersonaCorbata.png"));
        availablePersons.put("p2", new EntityPerson("p2", 8, 5, "PersonaNaranja.png"));
        availablePersons.put("p3", new EntityPerson("p3", 10, 3, "mujer.png"));
        availablePersons.put("p4", new EntityPerson("p4", 12, 7, "mujer1.png"));
        availablePersons.put("p5", new EntityPerson("p5", 5, 5, "personaCampesino.png"));
        availablePersons.put("p6", new EntityPerson("p6", 2, 4, "personaEstudiante.png"));
        availablePersons.put("p7", new EntityPerson("p7", 6, 9, "personaVerde.png"));
        availablePersons.put("p8", new EntityPerson("p8", 3, 4, "tombo.png"));
        availablePersons.put("p9", new EntityPerson("p9", 14, 9, "tombo1.png"));
    }

    private void broadcastAvailablePersons() {
        try {
            Map<String, Object> broadcast = new HashMap<>();
            Map<String, Object> personsList = new HashMap<>();

            availablePersons.forEach((id, person) -> {
                Map<String, Object> personData = new HashMap<>();
                personData.put("id", person.getId());
                personData.put("x", person.getX());
                personData.put("y", person.getY());
                personData.put("file", person.getSpriteFile());
                personsList.put(id, personData);
            });

            broadcast.put("type", "availablePersons");
            broadcast.put("persons", personsList);

            String jsonMessage = objectMapper.writeValueAsString(broadcast);
            TextMessage message = new TextMessage(jsonMessage);

            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    logger.warning("Error broadcasting available persons: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.severe("Error creating available persons broadcast: " + e.getMessage());
        }
    }

    private void handlePersonCollected(String playerId, String personId) {
        EntityPerson person = availablePersons.remove(personId);
        if (person != null) {
            logger.info("Person " + personId + " collected by player " + playerId);
            broadcastAvailablePersons();
        }
    }

    private void updatePlayer(String playerId, JSONObject data) {
        User player = players.get(playerId);
        if (player != null) {
            if (data.has("x"))
                player.setX(data.getDouble("x"));
            if (data.has("y"))
                player.setY(data.getDouble("y"));
            if (data.has("direction"))
                player.setDirection(data.getString("direction"));
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("message", errorMessage);

            String jsonError = objectMapper.writeValueAsString(error);
            session.sendMessage(new TextMessage(jsonError));
        } catch (IOException e) {
            logger.severe("Error sending error message: " + e.getMessage());
        }
    }

    private void broadcastPlayerStates() {
        JSONObject gameState = new JSONObject();
        JSONObject playersState = new JSONObject();

        players.forEach((id, player) -> {
            JSONObject playerData = new JSONObject();
            playerData.put("x", player.getX());
            playerData.put("y", player.getY());
            playerData.put("direction", player.getDirection());
            playerData.put("hasPerson", String.valueOf(player.getHasPerson()));
            playersState.put(id, playerData);
        });

        gameState.put("players", playersState);
        TextMessage message = new TextMessage(gameState.toString());

        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                    logger.info(message.getPayload());
                }
            } catch (IOException e) {
                logger.warning("Error sending message to " + session.getId() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        String playerId = sessionToPlayerId.get(sessionId);

        sessions.remove(playerId);
        players.remove(playerId);
        sessionToPlayerId.remove(sessionId);

        logger.info("Player " + playerId + " disconnected");
        broadcastPlayerStates();
    }

    private void broadcastPlayerPositions() {
        try {
            Map<String, Object> broadcast = new HashMap<>();
            Map<String, Object> positions = new HashMap<>();

            // Recolectar posiciones de todos los jugadores
            players.forEach((id, player) -> {
                Map<String, Object> position = new HashMap<>();
                position.put("x", player.getX());
                position.put("y", player.getY());
                position.put("direction", player.getDirection());
                positions.put(id, position);
            });

            broadcast.put("type", "positions");
            broadcast.put("players", positions);

            // Convertir a JSON y enviar
            String jsonMessage = objectMapper.writeValueAsString(broadcast);
            TextMessage message = new TextMessage(jsonMessage);

            // Enviar a todas las sesiones activas
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    logger.warning("Error broadcasting to session " + session.getId() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.severe("Error broadcasting positions: " + e.getMessage());
        }
    }

    private void handlePersonUpdate(String payload) {
        try {
            JSONObject data = new JSONObject(payload);
            String targetPlayerId;
            String hasPerson;

            // Handle both frontend formats
            if (data.has("id")) {
                targetPlayerId = data.getString("id");
            } else {
                targetPlayerId = data.getString("playerId");
            }

            // Handle both string and integer hasPerson values
            if (data.has("hasPerson")) {
                Object hasPersonValue = data.get("hasPerson");
                if (hasPersonValue instanceof String) {
                    hasPerson = (String) hasPersonValue;
                } else if (hasPersonValue instanceof Integer) {
                    hasPerson = String.valueOf(hasPersonValue);
                } else {
                    throw new IllegalArgumentException("Invalid hasPerson value type");
                }
            } else {
                throw new IllegalArgumentException("Missing hasPerson value");
            }

            User player = players.get(targetPlayerId);
            if (player != null) {
                player.setHasPerson(hasPerson);
                broadcastPersonStatus(targetPlayerId, hasPerson);
                logger.info("Updated hasPerson status for player " + targetPlayerId + " to " + hasPerson);
            } else {
                logger.warning("Player not found with ID: " + targetPlayerId);
            }
        } catch (Exception e) {
            logger.severe("Error processing person update: " + e.getMessage());
        }
    }

    private void broadcastPersonStatus(String playerId, String hasPerson) {
        try {
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("type", "personUpdate");
            broadcast.put("playerId", playerId);
            broadcast.put("hasPerson", hasPerson);

            String jsonMessage = objectMapper.writeValueAsString(broadcast);
            TextMessage message = new TextMessage(jsonMessage);

            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                        logger.info("Broadcast person status: " + jsonMessage);
                    }
                } catch (IOException e) {
                    logger.warning("Error broadcasting to session " + session.getId() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.severe("Error broadcasting person status: " + e.getMessage());
        }
    }

    private void broadcastPlayersCountToAdmins() {
        int count = players.size();
        JSONObject response = new JSONObject();
        response.put("type", "playersInfo");
        response.put("count", count);

        List<JSONObject> playersList = new ArrayList<>();
        players.forEach((plate, user) -> {
            JSONObject playerObj = new JSONObject();
            playerObj.put("plate", plate);
            playerObj.put("name", user.getName());
            playersList.add(playerObj);
        });
        response.put("players", playersList);
        TextMessage message = new TextMessage(response.toString());

        sessions.forEach((playerId, sess) -> {
            String role = sessionRoles.getOrDefault(playerId, "user");
            if ("admin".equalsIgnoreCase(role) && sess.isOpen()) {
                try {
                    sess.sendMessage(message);
                } catch (IOException e) {
                    logger.warning("Error enviando Info a admin " + playerId+ ": " + e.getMessage());
                }
            }
        });
    }

    private void sendPlayersCountToAdmin(WebSocketSession session) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", "playersInfo");
        response.put("count", players.size());

        List<JSONObject> playersList = new ArrayList<>();
        players.forEach((plate, user) -> {
            JSONObject playerObj = new JSONObject();
            playerObj.put("plate", plate);
            playerObj.put("name", user.getName());
            playersList.add(playerObj);
        });

        response.put("players", playersList);

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(response.toString()));
        } else {
            logger.info("Sesión admin no abierta, no se envió info");
        }
    }

    private String generatePlate() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numbers = "0123456789";
        StringBuilder plate = new StringBuilder();

        // Genera 3 números
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            plate.append(numbers.charAt(random.nextInt(numbers.length())));
        }

        // Genera 3 letras
        for (int i = 0; i < 3; i++) {
            plate.append(letters.charAt(random.nextInt(letters.length())));
        }

        return plate.toString();
    }

}
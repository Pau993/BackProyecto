package juego.arsw;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import juego.arsw.controller.UserRestController;
import juego.arsw.model.EntityPerson;
import juego.arsw.model.User;

@ExtendWith(MockitoExtension.class)
public class UserRestControllerTest {

    @InjectMocks
    private UserRestController controller;

    @Mock
    private WebSocketSession mockSession;

    @Captor
    private ArgumentCaptor<TextMessage> messageCaptor;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        when(mockSession.getId()).thenReturn("test-session-id");
    }

    @Test
    public void testAfterConnectionEstablished() throws Exception {
        // Arrange
        when(mockSession.isOpen()).thenReturn(true);
        
        // Act
        controller.afterConnectionEstablished(mockSession);
        
        // Assert
        verify(mockSession, times(3)).sendMessage(messageCaptor.capture());
        
        // Verify PLAYER_ID message
        TextMessage firstMessage = messageCaptor.getAllValues().get(0);
        JsonNode firstMessageNode = objectMapper.readTree(firstMessage.getPayload());
        assertEquals("PLAYER_ID", firstMessageNode.get("type").asText());
        assertNotNull(firstMessageNode.get("playerId").asText());
        
        // Verify availablePersons message
        TextMessage secondMessage = messageCaptor.getAllValues().get(1);
        JsonNode secondMessageNode = objectMapper.readTree(secondMessage.getPayload());
        assertEquals("availablePersons", secondMessageNode.get("type").asText());
        assertTrue(secondMessageNode.has("persons"));
    }

    @Test
    public void testHandleTextMessageForUserUpdate() throws Exception {
        // Arrange
        setupInitialConnection();
        JSONObject updateData = new JSONObject();
        updateData.put("x", 10.5);
        updateData.put("y", 20.3);
        updateData.put("direction", "right");
        
        // Act
        controller.handleTextMessage(mockSession, new TextMessage(updateData.toString()));
        
        // Assert
        verify(mockSession, atLeastOnce()).sendMessage(messageCaptor.capture());
        
        // Get relevant messages
        TextMessage positionsMessage = findMessageByType("positions");
        assertNotNull(positionsMessage);
        
        // Verify the player position was updated
        JsonNode positionsNode = objectMapper.readTree(positionsMessage.getPayload());
        JsonNode playersNode = positionsNode.get("players");
        assertNotNull(playersNode);
        
        // Get the player ID from session mapping
        Map<String, String> sessionToPlayerId = getSessionToPlayerIdMap();
        String playerId = sessionToPlayerId.get("test-session-id");
        
        // Check if the player position was updated correctly
        JsonNode playerNode = playersNode.get(playerId);
        assertNotNull(playerNode);
        assertEquals(10.5, playerNode.get("x").asDouble());
        assertEquals(20.3, playerNode.get("y").asDouble());
        assertEquals("right", playerNode.get("direction").asText());
    }

    @Test
    public void testHandlePersonCollected() throws Exception {
        // Arrange
        setupInitialConnection();
        
        // Initialize persons
        Map<String, EntityPerson> availablePersons = new ConcurrentHashMap<>();
        availablePersons.put("p1", new EntityPerson("p1", 4, 6, "PersonaCorbata.png"));
        ReflectionTestUtils.setField(controller, "availablePersons", availablePersons);
        
        JSONObject collectData = new JSONObject();
        collectData.put("type", "collectPerson");
        collectData.put("personId", "p1");
        
        // Act
        controller.handleTextMessage(mockSession, new TextMessage(collectData.toString()));
        
        // Assert
        verify(mockSession, atLeastOnce()).sendMessage(messageCaptor.capture());
        
        // Find availablePersons message
        TextMessage personsMessage = findMessageByType("availablePersons");
        assertNotNull(personsMessage);
        
        // Verify person was removed
        JsonNode personsNode = objectMapper.readTree(personsMessage.getPayload());
        JsonNode availablePersonsNode = personsNode.get("persons");
        assertFalse(availablePersonsNode.has("p1"));
    }

    @Test
    public void testAfterConnectionClosed() throws Exception {
    // Arrange
        when(mockSession.getId()).thenReturn("test-session-id");
        when(mockSession.isOpen()).thenReturn(true);
        controller.afterConnectionEstablished(mockSession);
        clearInvocations(mockSession); // Limpiar interacciones previas
    
    // Get the player ID from session mapping
        Map<String, String> sessionToPlayerId = getSessionToPlayerIdMap();
        String playerId = sessionToPlayerId.get("test-session-id");
    
    // Act
        controller.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
    
    // Assert
        Map<String, WebSocketSession> sessions = getSessionsMap();
        Map<String, User> players = getPlayersMap();
    
        assertFalse(sessions.containsKey(playerId));
        assertFalse(players.containsKey(playerId));
        assertFalse(sessionToPlayerId.containsKey("test-session-id"));
    }

    @Test
    public void testHandlePersonStateUpdate() throws Exception {
        // Arrange
        setupInitialConnection();
        
        // Initialize persons
        Map<String, EntityPerson> availablePersons = new ConcurrentHashMap<>();
        availablePersons.put("p1", new EntityPerson("p1", 4, 6, "PersonaCorbata.png"));
        ReflectionTestUtils.setField(controller, "availablePersons", availablePersons);
        
        JSONObject updateData = new JSONObject();
        updateData.put("personId", "p1");
        updateData.put("active", false);
        
        // Act
        controller.handleTextMessage(mockSession, new TextMessage(updateData.toString()));
        
        // Assert
        verify(mockSession, atLeastOnce()).sendMessage(messageCaptor.capture());
        
        // Find personStateUpdate message
        TextMessage stateUpdateMessage = findMessageByType("personStateUpdate");
        assertNotNull(stateUpdateMessage);
        
        // Verify state update message
        JsonNode stateUpdateNode = objectMapper.readTree(stateUpdateMessage.getPayload());
        assertEquals("p1", stateUpdateNode.get("personId").asText());
        assertFalse(stateUpdateNode.get("active").asBoolean());
        
        // Verify person was removed
        Map<String, EntityPerson> updatedPersons = getAvailablePersonsMap();
        assertFalse(updatedPersons.containsKey("p1"));
    }

    @Test
    public void testAdminConnection() throws Exception {
    // Arrange - Configuración directa para este test
        when(mockSession.getId()).thenReturn("test-session-id");
        when(mockSession.isOpen()).thenReturn(true);
    
    // Establecer conexión inicial
        controller.afterConnectionEstablished(mockSession);
    
    // Limpiar las interacciones previas para no confundir las verificaciones
        clearInvocations(mockSession);
    
    // NO reconfiguramos isOpen() aquí, ya que no es necesario y causa el error
    // when(mockSession.isOpen()).thenReturn(true); <- ELIMINAR ESTA LÍNEA
    
    // Preparar datos para simular mensaje de admin
        JSONObject adminData = new JSONObject();
        adminData.put("role", "admin");
    
    // Act
        controller.handleTextMessage(mockSession, new TextMessage(adminData.toString()));
    
    // Assert
        verify(mockSession, atLeastOnce()).sendMessage(messageCaptor.capture());
    
    // Find playersCount message
        TextMessage countMessage = findMessageByType("playersCount");
        assertNotNull(countMessage);
    
    // Verify count message
        JsonNode countNode = objectMapper.readTree(countMessage.getPayload());
        assertTrue(countNode.has("count"));
        assertEquals(0, countNode.get("count").asInt());
    
    // Verify role is set to admin
        Map<String, String> sessionRoles = getSessionRolesMap();
        Map<String, String> sessionToPlayerId = getSessionToPlayerIdMap();
        String playerId = sessionToPlayerId.get("test-session-id");
        assertEquals("admin", sessionRoles.get(playerId));
    }

    // Helper methods

    private void setupInitialConnection() throws Exception {
        when(mockSession.isOpen()).thenReturn(true);
        controller.afterConnectionEstablished(mockSession);
        reset(mockSession);
        when(mockSession.getId()).thenReturn("test-session-id");
        when(mockSession.isOpen()).thenReturn(true);
    }

    private TextMessage findMessageByType(String type) throws Exception {
        for (TextMessage message : messageCaptor.getAllValues()) {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if (node.has("type") && type.equals(node.get("type").asText())) {
                return message;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, WebSocketSession> getSessionsMap() {
        return (Map<String, WebSocketSession>) ReflectionTestUtils.getField(controller, "sessions");
    }

    @SuppressWarnings("unchecked")
    private Map<String, User> getPlayersMap() {
        return (Map<String, User>) ReflectionTestUtils.getField(controller, "players");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getSessionToPlayerIdMap() {
        return (Map<String, String>) ReflectionTestUtils.getField(controller, "sessionToPlayerId");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getSessionRolesMap() {
        return (Map<String, String>) ReflectionTestUtils.getField(controller, "sessionRoles");
    }

    @SuppressWarnings("unchecked")
    private Map<String, EntityPerson> getAvailablePersonsMap() {
        return (Map<String, EntityPerson>) ReflectionTestUtils.getField(controller, "availablePersons");
    }
}
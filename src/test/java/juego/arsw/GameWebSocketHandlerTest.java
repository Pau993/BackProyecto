package juego.arsw;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import juego.arsw.handler.GameWebSocketHandler;
import juego.arsw.model.User;
import juego.arsw.service.UserService;

@ExtendWith(MockitoExtension.class)
public class GameWebSocketHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private WebSocketSession session;

    @Mock
    private WebSocketSession anotherSession;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GameWebSocketHandler handler;

    @Captor
    private ArgumentCaptor<TextMessage> messageCaptor;

    private User testUser;
    private User processedUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("123");
        testUser.setName("TestUser");
        testUser.setX(10.5);
        testUser.setY(20.3);
        testUser.setDirection("left");
        testUser.setHasPerson("false");

        processedUser = new User("123");
        processedUser.setName("ProcessedUser");
        processedUser.setX(15.0);
        processedUser.setY(25.0);
        processedUser.setDirection("right");
        processedUser.setHasPerson("true");
    }

    @Test
    public void testAfterConnectionEstablished() throws Exception {
        // Arrange
        when(session.getId()).thenReturn("session-123");
        when(session.isOpen()).thenReturn(true);

        // Act
        handler.afterConnectionEstablished(session);

        // Assert
        verify(session).sendMessage(messageCaptor.capture());
        
        TextMessage capturedMessage = messageCaptor.getValue();
        JsonNode messageJson = objectMapper.readTree(capturedMessage.getPayload());
        
        assertEquals("usersList", messageJson.get("type").asText());
        assertTrue(messageJson.has("users"));
        
        // Verify session was added to the sessions map
        Map<String, WebSocketSession> sessions = getSessions();
        assertTrue(sessions.containsKey("session-123"));
        assertEquals(session, sessions.get("session-123"));
    }

    @Test
    public void testHandleTextMessage() throws Exception {
        // Arrange
        setupConnection();
        
        when(session.isOpen()).thenReturn(true);
        String userJson = objectMapper.writeValueAsString(testUser);
        TextMessage message = new TextMessage(userJson);
        
        List<User> allUsers = new ArrayList<>();
        allUsers.add(processedUser);
        
        when(userService.processUser(any(User.class))).thenReturn(processedUser);
        when(userService.getAllUsers()).thenReturn(allUsers);

        // Act
        handler.handleTextMessage(session, message);

        // Assert
        verify(userService).processUser(any(User.class));
        verify(session, atLeast(1)).sendMessage(messageCaptor.capture());
        
        // Verify the user was added to the users map
        Map<String, User> users = getUsers();
        assertTrue(users.containsKey("session-123"));
        assertEquals(processedUser, users.get("session-123"));
        
        // Verify the message contains the correct data
        TextMessage lastMessage = messageCaptor.getValue();
        JsonNode messageJson = objectMapper.readTree(lastMessage.getPayload());
        assertEquals("usersList", messageJson.get("type").asText());
        assertTrue(messageJson.has("users"));
        assertTrue(messageJson.get("users").isArray());
        assertEquals(1, messageJson.get("users").size());
    }

    @Test
    public void testHandleTextMessageWithMultipleSessions() throws Exception {
        // Arrange
        setupConnection();
        setupAdditionalConnection();
        
        when(session.isOpen()).thenReturn(true);
        when(anotherSession.isOpen()).thenReturn(true);
        
        String userJson = objectMapper.writeValueAsString(testUser);
        TextMessage message = new TextMessage(userJson);
        
        List<User> allUsers = new ArrayList<>();
        allUsers.add(processedUser);
        
        when(userService.processUser(any(User.class))).thenReturn(processedUser);
        when(userService.getAllUsers()).thenReturn(allUsers);

        // Act
        handler.handleTextMessage(session, message);

        // Assert
        verify(session, atLeast(1)).sendMessage(any(TextMessage.class));
        verify(anotherSession, atLeast(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    public void testAfterConnectionClosed() throws Exception {
        // Arrange
        setupConnection();
        addUserToMap("session-123", testUser);

        // Act
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Assert
        Map<String, WebSocketSession> sessions = getSessions();
        Map<String, User> users = getUsers();
        
        assertFalse(sessions.containsKey("session-123"));
        assertFalse(users.containsKey("session-123"));
        
        // Verify notification was sent (but no session to receive it in this case)
        // This is testing that no exceptions were thrown
        assertTrue(true); 
    }

    @Test
    public void testAfterConnectionClosedWithRemainingSession() throws Exception {
        // Arrange
        setupConnection();
        setupAdditionalConnection();
        addUserToMap("session-123", testUser);
        
        when(anotherSession.isOpen()).thenReturn(true);

        // Act
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Assert
        verify(anotherSession).sendMessage(any(TextMessage.class));
        
        Map<String, WebSocketSession> sessions = getSessions();
        Map<String, User> users = getUsers();
        
        assertFalse(sessions.containsKey("session-123"));
        assertFalse(users.containsKey("session-123"));
        assertTrue(sessions.containsKey("session-456"));
    }

    // Helper methods

    private void setupConnection() throws Exception {
        when(session.getId()).thenReturn("session-123");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        clearInvocations(session);
    }

    private void setupAdditionalConnection() throws Exception {
        when(anotherSession.getId()).thenReturn("session-456");
        when(anotherSession.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(anotherSession);
        clearInvocations(anotherSession);
    }

    private void addUserToMap(String sessionId, User user) {
        Map<String, User> users = getUsers();
        users.put(sessionId, user);
    }

    @SuppressWarnings("unchecked")
    private Map<String, WebSocketSession> getSessions() {
        return (Map<String, WebSocketSession>) ReflectionTestUtils.getField(handler, "sessions");
    }

    @SuppressWarnings("unchecked")
    private Map<String, User> getUsers() {
        return (Map<String, User>) ReflectionTestUtils.getField(handler, "users");
    }
}
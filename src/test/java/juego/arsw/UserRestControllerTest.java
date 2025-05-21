package juego.arsw;

import com.fasterxml.jackson.databind.ObjectMapper;

import juego.arsw.controller.UserRestController;
import juego.arsw.model.EntityPerson;
import juego.arsw.model.User;

import org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRestControllerTest {

    private UserRestController controller;
    private WebSocketSession mockSession;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        controller = new UserRestController();
        mockSession = mock(WebSocketSession.class);
        objectMapper = new ObjectMapper();

        // Configurar el comportamiento básico del mockSession
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);
    }

    private void resetControllerState() throws Exception {
        // Limpiar los mapas internos para cada prueba
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field playersField = UserRestController.class.getDeclaredField("players");
        Field availablePersonsField = UserRestController.class.getDeclaredField("availablePersons");
        Field sessionRolesField = UserRestController.class.getDeclaredField("sessionRoles");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");

        sessionsField.setAccessible(true);
        playersField.setAccessible(true);
        availablePersonsField.setAccessible(true);
        sessionRolesField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);

        sessionsField.set(controller, new ConcurrentHashMap<>());
        playersField.set(controller, new ConcurrentHashMap<>());
        availablePersonsField.set(controller, new ConcurrentHashMap<>());
        sessionRolesField.set(controller, new ConcurrentHashMap<>());
        sessionToPlayerIdField.set(controller, new ConcurrentHashMap<>());
    }

    @Test
    void testHandleTextMessage_NewPlayer() throws Exception {
        resetControllerState();

        // Configurar manualmente sin llamar a afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar mapas internos directamente
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");

        sessionsField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);

        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);

        // Crear mensaje para el nuevo jugador
        JSONObject playerData = new JSONObject();
        playerData.put("name", "TestPlayer");
        playerData.put("x", 10);
        playerData.put("y", 20);
        playerData.put("direction", "north");

        TextMessage playerMessage = new TextMessage(playerData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, playerMessage);

        // Verificar que el jugador fue añadido
        Field playersField = UserRestController.class.getDeclaredField("players");
        playersField.setAccessible(true);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        assertTrue(players.containsKey(playerId));
        User player = players.get(playerId);
        assertEquals("TestPlayer", player.getName()); // Ahora debería usar el nombre enviado
        assertEquals("0", player.getHasPerson());
    }

    @Test
    void testHandleTextMessage_AdminRole() throws Exception {
        resetControllerState();

        // Configurar manualmente sin llamar a afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar mapas internos directamente
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");

        sessionsField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);

        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);

        // Crear mensaje para rol admin
        JSONObject adminData = new JSONObject();
        adminData.put("role", "admin");

        TextMessage adminMessage = new TextMessage(adminData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, adminMessage);

        // Verificar que se asignó el rol admin
        Field sessionRolesField = UserRestController.class.getDeclaredField("sessionRoles");
        sessionRolesField.setAccessible(true);
        Map<String, String> sessionRoles = (Map<String, String>) sessionRolesField.get(controller);

        assertEquals("admin", sessionRoles.get(playerId));

        // Verificar que se envía información de jugadores al admin
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_UpdatePlayerPosition() throws Exception {
        resetControllerState();

        // Configurar manualmente sin llamar a afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar mapas internos directamente
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");
        Field playersField = UserRestController.class.getDeclaredField("players");

        sessionsField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);
        playersField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        // Crear un jugador inicial
        User player = new User(playerId);
        player.setName("TestPlayer");
        player.setX(10);
        player.setY(10);
        player.setDirection("south");

        // Añadir jugador y mapeos
        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);
        players.put(playerId, player);

        // Crear mensaje para actualizar posición
        JSONObject updateData = new JSONObject();
        updateData.put("x", 20);
        updateData.put("y", 30);
        updateData.put("direction", "east");

        TextMessage updateMessage = new TextMessage(updateData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, updateMessage);

        // Verificar que la posición fue actualizada
        assertEquals(20, player.getX());
        assertEquals(30, player.getY());
        assertEquals("east", player.getDirection());
    }

    @Test
    void testHandleTextMessage_PersonStateUpdate() throws Exception {
        resetControllerState();
        controller.afterConnectionEstablished(mockSession);

        // Inicializar personas disponibles manualmente
        Field availablePersonsField = UserRestController.class.getDeclaredField("availablePersons");
        availablePersonsField.setAccessible(true);
        Map<String, EntityPerson> availablePersons = (Map<String, EntityPerson>) availablePersonsField.get(controller);
        availablePersons.put("p1", new EntityPerson("p1", 4, 6, "PersonaCorbata.png"));

        reset(mockSession);
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Crear mensaje para actualizar estado de persona
        JSONObject personUpdateData = new JSONObject();
        personUpdateData.put("personId", "p1");
        personUpdateData.put("active", false);

        TextMessage personUpdateMessage = new TextMessage(personUpdateData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, personUpdateMessage);

        // Verificar que la persona fue removida
        assertFalse(availablePersons.containsKey("p1"));
    }

    @Test
    void testHandleTextMessage_CollectPerson() throws Exception {
        resetControllerState();

        // Configurar manualmente sin llamar a afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar mapas internos directamente
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");

        sessionsField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);

        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);

        // Inicializar personas disponibles manualmente
        Field availablePersonsField = UserRestController.class.getDeclaredField("availablePersons");
        availablePersonsField.setAccessible(true);
        Map<String, EntityPerson> availablePersons = (Map<String, EntityPerson>) availablePersonsField.get(controller);
        availablePersons.put("p2", new EntityPerson("p2", 8, 5, "PersonaNaranja.png"));

        // Crear mensaje para recolectar persona
        JSONObject collectData = new JSONObject();
        collectData.put("type", "collectPerson");
        collectData.put("personId", "p2");

        TextMessage collectMessage = new TextMessage(collectData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, collectMessage);

        // Verificar que la persona fue removida
        assertFalse(availablePersons.containsKey("p2"));
    }

    @Test
    void testHandleTextMessage_UpdateHasPerson() throws Exception {
        resetControllerState();

        // Configurar manualmente sin llamar a afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar mapas internos directamente
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");
        Field playersField = UserRestController.class.getDeclaredField("players");

        sessionsField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);
        playersField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        // Crear un jugador inicial
        User player = new User(playerId);
        player.setName("TestPlayer");
        player.setHasPerson("0");

        // Añadir jugador y mapeos
        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);
        players.put(playerId, player);

        // Crear mensaje para actualizar hasPerson
        JSONObject updatePersonData = new JSONObject();
        updatePersonData.put("id", playerId);
        updatePersonData.put("hasPerson", "1");

        TextMessage updatePersonMessage = new TextMessage(updatePersonData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, updatePersonMessage);

        // Verificar que hasPerson fue actualizado
        assertEquals("1", player.getHasPerson());
    }

    @Test
    void testHandleTextMessage_UpdateHasPersonAlternateFormat() throws Exception {
        resetControllerState();

        // Configurar manualmente sin llamar a afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar mapas internos directamente
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");
        Field playersField = UserRestController.class.getDeclaredField("players");

        sessionsField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);
        playersField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        // Crear un jugador inicial
        User player = new User(playerId);
        player.setName("TestPlayer");
        player.setHasPerson("0");

        // Añadir jugador y mapeos
        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);
        players.put(playerId, player);

        // Crear mensaje para actualizar hasPerson (formato alternativo)
        JSONObject updatePersonData = new JSONObject();
        updatePersonData.put("playerId", playerId);
        updatePersonData.put("hasPerson", 1); // Formato de entero

        TextMessage updatePersonMessage = new TextMessage(updatePersonData.toString());

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, updatePersonMessage);

        // Verificar que hasPerson fue actualizado
        assertEquals("1", player.getHasPerson());
    }

    @Test
    void testHandleTextMessage_InvalidMessage() throws Exception {
        resetControllerState();
        controller.afterConnectionEstablished(mockSession);

        reset(mockSession);
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Crear mensaje inválido (formato incorrecto)
        TextMessage invalidMessage = new TextMessage("This is not valid JSON");

        // Ejecutar el método bajo prueba
        controller.handleTextMessage(mockSession, invalidMessage);

        // Verificar que se envió un mensaje de error
        verify(mockSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        resetControllerState();

        // Configurar el estado manualmente en lugar de llamar a
        // afterConnectionEstablished
        String playerId = "123TEST"; // ID fijo para la prueba
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Configurar manualmente los mapas internos
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        Field playersField = UserRestController.class.getDeclaredField("players");
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");

        sessionsField.setAccessible(true);
        playersField.setAccessible(true);
        sessionToPlayerIdField.setAccessible(true);

        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);

        sessions.put(playerId, mockSession);
        sessionToPlayerId.put("session-id-123", playerId);

        // Crear un jugador y añadirlo
        User player = new User(playerId);
        players.put(playerId, player);

        // Verificar que los mapas contienen las entradas esperadas
        assertTrue(sessions.containsKey(playerId));
        assertTrue(sessionToPlayerId.containsKey("session-id-123"));

        // Ejecutar el método bajo prueba
        controller.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Verificar que se eliminaron las referencias
        assertFalse(sessions.containsKey(playerId));
        assertFalse(players.containsKey(playerId));
        assertFalse(sessionToPlayerId.containsKey("session-id-123"));
    }

    @Test
    void testGeneratePlate() throws Exception {
        // Acceder al método privado
        java.lang.reflect.Method generatePlateMethod = UserRestController.class.getDeclaredMethod("generatePlate");
        generatePlateMethod.setAccessible(true);

        // Invocar el método privado
        String plate = (String) generatePlateMethod.invoke(controller);

        // Verificar el formato general
        assertNotNull(plate);
        assertEquals(7, plate.length()); // 3 letras + '-' + 3 números
        assertEquals('-', plate.charAt(3), "El cuarto carácter debe ser un guion '-'");

        // Verificar letras
        for (int i = 0; i < 3; i++) {
            assertTrue(Character.isUpperCase(plate.charAt(i)), "Los primeros 3 caracteres deben ser letras mayúsculas");
        }

        // Verificar números
        for (int i = 4; i < 7; i++) {
            assertTrue(Character.isDigit(plate.charAt(i)), "Los últimos 3 caracteres deben ser dígitos");
        }
    }

    @Test
    void testBroadcastingFunctions() throws Exception {
        resetControllerState();

        // Configurar múltiples sesiones mock
        WebSocketSession mockSession1 = mock(WebSocketSession.class);
        WebSocketSession mockSession2 = mock(WebSocketSession.class);

        when(mockSession1.getId()).thenReturn("session-id-1");
        when(mockSession2.getId()).thenReturn("session-id-2");
        when(mockSession1.isOpen()).thenReturn(true);
        when(mockSession2.isOpen()).thenReturn(true);

        // Añadir sesiones al mapa
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        sessions.put("123ABC", mockSession1);
        sessions.put("456DEF", mockSession2);

        // Configurar jugadores
        Field playersField = UserRestController.class.getDeclaredField("players");
        playersField.setAccessible(true);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        User player1 = new User("123ABC");
        player1.setName("Player1");
        player1.setX(10);
        player1.setY(20);
        player1.setDirection("north");

        User player2 = new User("456DEF");
        player2.setName("Player2");
        player2.setX(30);
        player2.setY(40);
        player2.setDirection("south");

        players.put("123ABC", player1);
        players.put("456DEF", player2);

        // Configurar mapeo de sesiones a playerIds
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");
        sessionToPlayerIdField.setAccessible(true);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);
        sessionToPlayerId.put("session-id-1", "123ABC");
        sessionToPlayerId.put("session-id-2", "456DEF");

        // Probar broadcastPlayerPositions
        java.lang.reflect.Method broadcastPositionsMethod = UserRestController.class
                .getDeclaredMethod("broadcastPlayerPositions");
        broadcastPositionsMethod.setAccessible(true);
        broadcastPositionsMethod.invoke(controller);

        // Verificar que se envió un mensaje a ambas sesiones
        verify(mockSession1, times(1)).sendMessage(any(TextMessage.class));
        verify(mockSession2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testBroadcastPersonStatus() throws Exception {
        resetControllerState();

        // Configurar sesiones mock
        WebSocketSession mockSession1 = mock(WebSocketSession.class);
        when(mockSession1.getId()).thenReturn("session-id-1");
        when(mockSession1.isOpen()).thenReturn(true);

        // Añadir sesión al mapa
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        sessions.put("123ABC", mockSession1);

        // Acceder al método privado
        java.lang.reflect.Method broadcastPersonStatusMethod = UserRestController.class
                .getDeclaredMethod("broadcastPersonStatus", String.class, String.class);
        broadcastPersonStatusMethod.setAccessible(true);

        // Invocar el método
        broadcastPersonStatusMethod.invoke(controller, "123ABC", "1");

        // Verificar que se envió un mensaje
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession1).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        JSONObject json = new JSONObject(payload);

        assertEquals("personUpdate", json.getString("type"));
        assertEquals("123ABC", json.getString("playerId"));
        assertEquals("1", json.getString("hasPerson"));
    }

    @Test
    void testBroadcastPlayersCountToAdmins() throws Exception {
        resetControllerState();

        // Configurar sesiones mock
        WebSocketSession adminSession = mock(WebSocketSession.class);
        WebSocketSession userSession = mock(WebSocketSession.class);

        when(adminSession.getId()).thenReturn("admin-session");
        when(userSession.getId()).thenReturn("user-session");
        when(adminSession.isOpen()).thenReturn(true);
        when(userSession.isOpen()).thenReturn(true);

        // Añadir sesiones al mapa
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        sessions.put("ADMIN123", adminSession);
        sessions.put("USER456", userSession);

        // Configurar roles
        Field sessionRolesField = UserRestController.class.getDeclaredField("sessionRoles");
        sessionRolesField.setAccessible(true);
        Map<String, String> sessionRoles = (Map<String, String>) sessionRolesField.get(controller);
        sessionRoles.put("ADMIN123", "admin");
        sessionRoles.put("USER456", "user");

        // Configurar jugadores
        Field playersField = UserRestController.class.getDeclaredField("players");
        playersField.setAccessible(true);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        User player = new User("USER456");
        player.setName("TestUser");
        players.put("USER456", player);

        // Acceder al método privado
        java.lang.reflect.Method broadcastCountMethod = UserRestController.class
                .getDeclaredMethod("broadcastPlayersCountToAdmins");
        broadcastCountMethod.setAccessible(true);

        // Invocar el método
        broadcastCountMethod.invoke(controller);

        // Verificar que solo se envió mensaje a la sesión admin
        verify(adminSession, times(1)).sendMessage(any(TextMessage.class));
        verify(userSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSendPlayersCountToAdmin() throws Exception {
        resetControllerState();

        // Configurar sesión mock
        WebSocketSession adminSession = mock(WebSocketSession.class);
        when(adminSession.isOpen()).thenReturn(true);

        // Configurar jugadores
        Field playersField = UserRestController.class.getDeclaredField("players");
        playersField.setAccessible(true);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);

        User player1 = new User("USER123");
        player1.setName("Player1");
        User player2 = new User("USER456");
        player2.setName("Player2");

        players.put("USER123", player1);
        players.put("USER456", player2);

        // Acceder al método privado
        java.lang.reflect.Method sendCountMethod = UserRestController.class.getDeclaredMethod("sendPlayersCountToAdmin",
                WebSocketSession.class);
        sendCountMethod.setAccessible(true);

        // Invocar el método
        sendCountMethod.invoke(controller, adminSession);

        // Verificar que se envió un mensaje con la información correcta
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(adminSession).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        JSONObject json = new JSONObject(payload);

        assertEquals("playersInfo", json.getString("type"));
        assertEquals(2, json.getInt("count"));
        assertTrue(json.has("players"));
    }

    @Test
    void testSendPlayersCountToAdmin_SessionClosed() throws Exception {
        resetControllerState();

        // Configurar sesión mock cerrada
        WebSocketSession closedSession = mock(WebSocketSession.class);
        when(closedSession.isOpen()).thenReturn(false);

        // Acceder al método privado
        java.lang.reflect.Method sendCountMethod = UserRestController.class.getDeclaredMethod("sendPlayersCountToAdmin",
                WebSocketSession.class);
        sendCountMethod.setAccessible(true);

        // Invocar el método
        sendCountMethod.invoke(controller, closedSession);

        // Verificar que no se intentó enviar mensaje
        verify(closedSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_ErrorHandling() throws Exception {
        resetControllerState();

        // Crear la configuración inicial sin lanzar excepciones
        when(mockSession.getId()).thenReturn("session-id-123");
        when(mockSession.isOpen()).thenReturn(true);

        // Preparar el mapa de sesiones directamente
        String playerId = "123TEST"; // ID fijo para la prueba
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        sessions.put(playerId, mockSession);

        // Añadir manualmente el jugador al mapa
        Field playersField = UserRestController.class.getDeclaredField("players");
        playersField.setAccessible(true);
        Map<String, User> players = (Map<String, User>) playersField.get(controller);
        User player = new User(playerId);
        players.put(playerId, player);

        // Añadir la asignación de sessionId a playerId
        Field sessionToPlayerIdField = UserRestController.class.getDeclaredField("sessionToPlayerId");
        sessionToPlayerIdField.setAccessible(true);
        Map<String, String> sessionToPlayerId = (Map<String, String>) sessionToPlayerIdField.get(controller);
        sessionToPlayerId.put("session-id-123", playerId);

        // Ahora configurar para que lance excepción al enviar mensaje
        doThrow(new IOException("Send error")).when(mockSession).sendMessage(any(TextMessage.class));

        // Crear mensaje para actualizar
        JSONObject updateData = new JSONObject();
        updateData.put("x", 20);
        updateData.put("y", 30);

        TextMessage updateMessage = new TextMessage(updateData.toString());

        // Ejecutar método - no debería lanzar excepción
        try {
            controller.handleTextMessage(mockSession, updateMessage);
            // Si llegamos aquí, el método manejó la excepción correctamente
            assertTrue(true);
        } catch (Exception e) {
            fail("El método no debería propagar excepciones: " + e.getMessage());
        }

        // Verificar que sendMessage fue llamado al menos una vez (no especificamos el
        // número exacto)
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testBroadcastAvailablePersons() throws Exception {
        resetControllerState();

        // Configurar sesiones mock
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.isOpen()).thenReturn(true);

        // Añadir sesiones al mapa
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        sessions.put("TEST123", mockSession);

        // Inicializar personas disponibles
        Field availablePersonsField = UserRestController.class.getDeclaredField("availablePersons");
        availablePersonsField.setAccessible(true);
        Map<String, EntityPerson> availablePersons = (Map<String, EntityPerson>) availablePersonsField.get(controller);
        availablePersons.put("p1", new EntityPerson("p1", 4, 6, "PersonaCorbata.png"));

        // Acceder al método privado
        java.lang.reflect.Method broadcastMethod = UserRestController.class
                .getDeclaredMethod("broadcastAvailablePersons");
        broadcastMethod.setAccessible(true);

        // Invocar el método
        broadcastMethod.invoke(controller);

        // Verificar que se envió un mensaje con la información correcta
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("availablePersons"));
        assertTrue(payload.contains("p1"));
    }

    @Test
    void testHandlePersonUpdate_PlayerNotFound() throws Exception {
        resetControllerState();

        // Acceder al método privado
        java.lang.reflect.Method handleUpdateMethod = UserRestController.class.getDeclaredMethod("handlePersonUpdate",
                String.class);
        handleUpdateMethod.setAccessible(true);

        // Crear payload con ID de jugador inexistente
        JSONObject updateData = new JSONObject();
        updateData.put("id", "NONEXISTENT");
        updateData.put("hasPerson", "1");

        // Invocar el método - no debería lanzar excepción
        handleUpdateMethod.invoke(controller, updateData.toString());

        // Si llegamos aquí, el método manejó correctamente el caso
        assertTrue(true);
    }

    @Test
    void testBroadcastAvailablePersons_ErrorHandling() throws Exception {
        resetControllerState();

        // Configurar sesión mock para lanzar excepción
        WebSocketSession errorSession = mock(WebSocketSession.class);
        when(errorSession.isOpen()).thenReturn(true);
        doThrow(new IOException("Test exception")).when(errorSession).sendMessage(any(TextMessage.class));

        // Añadir sesión al mapa
        Field sessionsField = UserRestController.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) sessionsField.get(controller);
        sessions.put("ERROR123", errorSession);

        // Acceder al método privado
        java.lang.reflect.Method broadcastMethod = UserRestController.class
                .getDeclaredMethod("broadcastAvailablePersons");
        broadcastMethod.setAccessible(true);

        // Invocar el método - no debería lanzar excepción
        try {
            broadcastMethod.invoke(controller);
            // Si llegamos aquí, el método manejó la excepción correctamente
            assertTrue(true);
        } catch (Exception e) {
            fail("El método no debería propagar excepciones: " + e.getMessage());
        }
    }
}
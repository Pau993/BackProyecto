package juego.arsw;

import juego.arsw.config.WebSocketConfig;
import juego.arsw.controller.UserRestController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebSocketConfigTest {

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Captor
    private ArgumentCaptor<WebSocketHandler> handlerCaptor;

    @Test
    public void testRegisterWebSocketHandlers() {
        // Arrange
        when(registry.addHandler(any(WebSocketHandler.class), anyString())).thenReturn(registration);
        
        // Act
        webSocketConfig.registerWebSocketHandlers(registry);
        
        // Assert
        verify(registry).addHandler(handlerCaptor.capture(), eq("/game"));
        verify(registration).setAllowedOrigins("*");
        
        WebSocketHandler capturedHandler = handlerCaptor.getValue();
        assertNotNull(capturedHandler);
        assertTrue(capturedHandler instanceof UserRestController);
    }

    @Test
    public void testUserWebSocketHandlerBean() {
        // Act
        UserRestController handler = webSocketConfig.userWebSocketHandler();
        
        // Assert
        assertNotNull(handler);
        assertTrue(handler instanceof UserRestController);
    }
}
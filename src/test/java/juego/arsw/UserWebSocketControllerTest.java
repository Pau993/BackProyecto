package juego.arsw;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import juego.arsw.controller.UserWebSocketController;
import juego.arsw.model.User;
import juego.arsw.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserWebSocketControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserWebSocketController controller;

    private User testUser;
    private User processedUser;

    @BeforeEach
    public void setUp() {
        // Preparar un usuario de prueba
        testUser = new User("123");
        testUser.setName("TestUser");
        testUser.setX(10.5);
        testUser.setY(20.3);
        testUser.setDirection("left");
        testUser.setHasPerson("false");

        // Preparar un usuario procesado que el servicio devolvería
        processedUser = new User("123");
        processedUser.setName("ProcessedUser");
        processedUser.setX(15.0);
        processedUser.setY(25.0);
        processedUser.setDirection("right");
        processedUser.setHasPerson("true");
    }

    @Test
    public void testProcessPlayer() {
        // Arrange
        when(userService.processUser(testUser)).thenReturn(processedUser);

        // Act
        User result = controller.processPlayer(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(processedUser.getId(), result.getId());
        assertEquals(processedUser.getName(), result.getName());
        assertEquals(processedUser.getX(), result.getX());
        assertEquals(processedUser.getY(), result.getY());
        assertEquals(processedUser.getDirection(), result.getDirection());
        assertEquals(processedUser.getHasPerson(), result.getHasPerson());

        // Verify the service was called with correct parameters
        verify(userService, times(1)).processUser(testUser);
    }

    @Test
    public void testProcessPlayerReturnsUnmodifiedUser() {
        // Arrange
        // Simulate case where service returns same user object without modifications
        when(userService.processUser(testUser)).thenReturn(testUser);

        // Act
        User result = controller.processPlayer(testUser);

        // Assert
        assertNotNull(result);
        assertSame(testUser, result); // Verify it's exactly the same object
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getX(), result.getX());
        assertEquals(testUser.getY(), result.getY());
        assertEquals(testUser.getDirection(), result.getDirection());
        assertEquals(testUser.getHasPerson(), result.getHasPerson());

        // Verify the service was called
        verify(userService, times(1)).processUser(testUser);
    }

    @Test
    public void testProcessPlayerWithNull() {
        // Arrange
        when(userService.processUser(null)).thenReturn(null);

        // Act
        User result = controller.processPlayer(null);

        // Assert
        assertNull(result);
        
        // Verify the service was called with null
        verify(userService, times(1)).processUser(null);
    }
}
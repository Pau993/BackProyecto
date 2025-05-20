package juego.arsw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import juego.arsw.controller.UserController;
import juego.arsw.model.User;
import juego.arsw.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("123");
        testUser.setName("TestUser");
        testUser.setX(10.5);
        testUser.setY(20.3);
        testUser.setDirection("left");
        testUser.setHasPerson("false");
    }

    @Test
    public void testSendUser() {
        // Arrange
        User processedUser = new User("123");
        processedUser.setName("ProcessedUser");
        processedUser.setX(15.0);
        processedUser.setY(25.0);
        processedUser.setDirection("right");
        processedUser.setHasPerson("true");

        when(userService.processUser(testUser)).thenReturn(processedUser);

        // Act
        User result = userController.sendUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(processedUser.getId(), result.getId());
        assertEquals(processedUser.getName(), result.getName());
        assertEquals(processedUser.getX(), result.getX());
        assertEquals(processedUser.getY(), result.getY());
        assertEquals(processedUser.getDirection(), result.getDirection());
        assertEquals(processedUser.getHasPerson(), result.getHasPerson());

        // Verify that the service method was called with the correct parameter
        verify(userService, times(1)).processUser(testUser);
    }

    @Test
    public void testSendUserIdentityCase() {
        // Arrange
        // Simulate case where user service returns the same user object
        when(userService.processUser(testUser)).thenReturn(testUser);

        // Act
        User result = userController.sendUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getX(), result.getX());
        assertEquals(testUser.getY(), result.getY());
        assertEquals(testUser.getDirection(), result.getDirection());
        assertEquals(testUser.getHasPerson(), result.getHasPerson());

        // Verify that the service method was called with the correct parameter
        verify(userService, times(1)).processUser(testUser);
    }

    @Test
    public void testSendUserWithNullUser() {
        // Arrange
        User nullUser = null;
        // This is necessary to handle the null case safely
        when(userService.processUser(null)).thenReturn(null);

        // Act
        User result = userController.sendUser(nullUser);

        // Assert
        assertEquals(null, result);
        
        // Verify service was called with null
        verify(userService, times(1)).processUser(null);
    }
}
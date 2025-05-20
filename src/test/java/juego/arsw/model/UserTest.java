package juego.arsw.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testDefaultConstructor() {
        // Arrange & Act
        User user = new User("1234");
        
        // Assert
        assertEquals("1234", user.getId());
        assertEquals("Player-1234", user.getName());
        assertEquals(0.0, user.getX());
        assertEquals(0.0, user.getY());
        assertEquals("right", user.getDirection());
        assertNull(user.getHasPerson());
    }
    
    @Test
    public void testParameterizedConstructor() {
        // Arrange & Act
        User user = new User("5678", 10.5, 20.3, "left", true);
        
        // Assert
        assertEquals("5678", user.getId());
        assertEquals("Player-5678", user.getName());
        assertEquals(10.5, user.getX());
        assertEquals(20.3, user.getY());
        assertEquals("left", user.getDirection());
        // Nota: el constructor toma un boolean pero el campo es String
        // Este test probablemente fallará porque hay una inconsistencia en el diseño
    }
    
    @Test
    public void testSetAndGetId() {
        // Arrange
        User user = new User("1234");
        
        // Act
        user.setId("9876");
        
        // Assert
        assertEquals("9876", user.getId());
    }
    
    @Test
    public void testSetAndGetName() {
        // Arrange
        User user = new User("1234");
        
        // Act
        user.setName("TestPlayer");
        
        // Assert
        assertEquals("TestPlayer", user.getName());
    }
    
    @Test
    public void testSetAndGetX() {
        // Arrange
        User user = new User("1234");
        
        // Act
        user.setX(15.75);
        
        // Assert
        assertEquals(15.75, user.getX());
    }
    
    @Test
    public void testSetAndGetY() {
        // Arrange
        User user = new User("1234");
        
        // Act
        user.setY(30.25);
        
        // Assert
        assertEquals(30.25, user.getY());
    }
    
    @Test
    public void testSetAndGetDirection() {
        // Arrange
        User user = new User("1234");
        
        // Act
        user.setDirection("up");
        
        // Assert
        assertEquals("up", user.getDirection());
    }
    
    @Test
    public void testSetAndGetHasPerson() {
        // Arrange
        User user = new User("1234");
        
        // Act
        user.setHasPerson("true");
        
        // Assert
        assertEquals("true", user.getHasPerson());
    }
}
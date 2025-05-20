package juego.arsw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import juego.arsw.model.EntityPerson;

public class EntityPersonTests {

    @Test
    public void testDefaultConstructor() {
        // Arrange & Act
        EntityPerson entity = new EntityPerson();
        
        // Assert
        assertNull(entity.getId());
        assertEquals(0, entity.getX());
        assertEquals(0, entity.getY());
        assertNull(entity.getSpriteFile());
    }
    
    @Test
    public void testParameterizedConstructor() {
        // Arrange & Act
        EntityPerson entity = new EntityPerson("1234", 10, 20, "sprite.png");
        
        // Assert
        assertEquals("1234", entity.getId());
        assertEquals(10, entity.getX());
        assertEquals(20, entity.getY());
        assertEquals("sprite.png", entity.getSpriteFile());
    }
    
    @Test
    public void testSetAndGetId() {
        // Arrange
        EntityPerson entity = new EntityPerson();
        
        // Act
        entity.setId("5678");
        
        // Assert
        assertEquals("5678", entity.getId());
    }
    
    @Test
    public void testSetAndGetX() {
        // Arrange
        EntityPerson entity = new EntityPerson();
        
        // Act
        entity.setX(15);
        
        // Assert
        assertEquals(15, entity.getX());
    }
    
    @Test
    public void testSetAndGetY() {
        // Arrange
        EntityPerson entity = new EntityPerson();
        
        // Act
        entity.setY(25);
        
        // Assert
        assertEquals(25, entity.getY());
    }
    
    @Test
    public void testSetAndGetSpriteFile() {
        // Arrange
        EntityPerson entity = new EntityPerson();
        
        // Act
        entity.setSpriteFile("new_sprite.png");
        
        // Assert
        assertEquals("new_sprite.png", entity.getSpriteFile());
    }
    
}

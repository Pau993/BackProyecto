package juego.arsw;

import juego.arsw.model.User;
import juego.arsw.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        testUser = new User();
        testUser.setId("test-id");
        testUser.setName("Test User");
        testUser.setX(10);
        testUser.setY(20);
        testUser.setDirection("north");
    }

    @Test
    void testCreateUser() {
        User createdUser = userService.createUser(testUser);
        
        assertEquals(testUser, createdUser);
        assertEquals(1, userService.getAllUsers().size());
        assertTrue(userService.getUserById("test-id").isPresent());
    }

    @Test
    void testGetUserById() {
        userService.createUser(testUser);
        
        Optional<User> foundUser = userService.getUserById("test-id");
        
        assertTrue(foundUser.isPresent());
        assertEquals("test-id", foundUser.get().getId());
        assertEquals("Test User", foundUser.get().getName());
        
        Optional<User> notFoundUser = userService.getUserById("non-existent");
        assertFalse(notFoundUser.isPresent());
    }

    @Test
    void testGetAllUsers() {
        userService.createUser(testUser);
        
        User secondUser = new User();
        secondUser.setId("second-id");
        secondUser.setName("Second User");
        userService.createUser(secondUser);
        
        List<User> allUsers = userService.getAllUsers();
        
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.contains(testUser));
        assertTrue(allUsers.contains(secondUser));
    }

    @Test
    void testUpdateUser() {
        userService.createUser(testUser);
        
        User updatedUser = new User();
        updatedUser.setName("Updated Name");
        updatedUser.setX(30);
        updatedUser.setY(40);
        updatedUser.setDirection("south");
        
        User result = userService.updateUser("test-id", updatedUser);
        
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("Updated Name", result.getName());
        assertEquals(30, result.getX());
        assertEquals(40, result.getY());
        assertEquals("south", result.getDirection());
        
        Optional<User> foundUser = userService.getUserById("test-id");
        assertTrue(foundUser.isPresent());
        assertEquals("Updated Name", foundUser.get().getName());
        
        // Update non-existent user
        User nullResult = userService.updateUser("non-existent", updatedUser);
        assertNull(nullResult);
    }

    @Test
    void testDeleteUser() {
        userService.createUser(testUser);
        
        boolean deleted = userService.deleteUser("test-id");
        
        assertTrue(deleted);
        assertEquals(0, userService.getAllUsers().size());
        assertFalse(userService.getUserById("test-id").isPresent());
        
        // Delete non-existent user
        boolean notDeleted = userService.deleteUser("non-existent");
        assertFalse(notDeleted);
    }

    @Test
    void testProcessUser_Create() {
        User newUser = new User();
        newUser.setId("new-id");
        newUser.setName("New User");
        
        User result = userService.processUser(newUser);
        
        assertEquals(newUser, result);
        assertEquals(1, userService.getAllUsers().size());
        assertTrue(userService.getUserById("new-id").isPresent());
    }

    @Test
    void testProcessUser_Update() {
        userService.createUser(testUser);
        
        User updatedUser = new User();
        updatedUser.setId("test-id");
        updatedUser.setName("Updated via Process");
        updatedUser.setX(50);
        updatedUser.setY(60);
        
        User result = userService.processUser(updatedUser);
        
        assertEquals("Updated via Process", result.getName());
        assertEquals(50, result.getX());
        assertEquals(60, result.getY());
        
        Optional<User> foundUser = userService.getUserById("test-id");
        assertTrue(foundUser.isPresent());
        assertEquals("Updated via Process", foundUser.get().getName());
    }

    @Test
    void testUpdateUserPosition() {
        userService.createUser(testUser);
        
        userService.updateUserPosition("test-id", 100, 200, "east");
        
        Optional<User> foundUser = userService.getUserById("test-id");
        assertTrue(foundUser.isPresent());
        assertEquals(100, foundUser.get().getX());
        assertEquals(200, foundUser.get().getY());
        assertEquals("east", foundUser.get().getDirection());
        
        // Update position of non-existent user (should not throw exception)
        userService.updateUserPosition("non-existent", 300, 400, "west");
    }

    @Test
    void testUpdateUserName() {
        userService.createUser(testUser);
        
        userService.updateUserName("test-id", "New Name");
        
        Optional<User> foundUser = userService.getUserById("test-id");
        assertTrue(foundUser.isPresent());
        assertEquals("New Name", foundUser.get().getName());
        
        // Update name of non-existent user (should not throw exception)
        userService.updateUserName("non-existent", "Another Name");
    }
}
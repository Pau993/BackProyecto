package juego.arsw;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import juego.arsw.controller.LoginController;

@ExtendWith(MockitoExtension.class)
public class LoginControllerTest {

    @Mock
    private HttpServletRequest mockRequest;

    @InjectMocks
    private LoginController loginController;

    private Map<String, String> userCredentials;

    @BeforeEach
    public void setUp() {
        userCredentials = new HashMap<>();
        // Eliminados los stubbings innecesarios de aquí
    }

    @Test
    public void testSuccessfulLogin() {
        // Arrange
        userCredentials.put("username", "andres");
        userCredentials.put("password", "1234");

        // Act
        ResponseEntity<?> response = loginController.login(userCredentials, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertNotNull(responseBody.get("token"));
    }

    @Test
    public void testLoginWithInvalidCredentials() {
        // Arrange
        userCredentials.put("username", "andres");
        userCredentials.put("password", "wrongPassword");

        // Act
        ResponseEntity<?> response = loginController.login(userCredentials, mockRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody());
    }

    @Test
    public void testLoginWithMissingCredentials() {
        // Arrange
        userCredentials.put("username", "andres");
        // password missing

        // Act
        ResponseEntity<?> response = loginController.login(userCredentials, mockRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Usuario y contraseña requeridos", response.getBody());
    }

    @Test
public void testAccountBlockAfterThreeFailedAttempts() {
    // Arrange
    userCredentials.put("username", "andres");
    userCredentials.put("password", "wrongPassword");
    // Eliminados los stubbings innecesarios
    
    // Act - 3 failed login attempts
    loginController.login(userCredentials, mockRequest);
    loginController.login(userCredentials, mockRequest);
    loginController.login(userCredentials, mockRequest);
    
    // 4th attempt should be blocked
    ResponseEntity<?> response = loginController.login(userCredentials, mockRequest);

    // Assert
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);
    
    @SuppressWarnings("unchecked")
    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
    assertEquals("Demasiados intentos fallidos. Intente más tarde.", responseBody.get("message"));
    assertTrue(((Number)responseBody.get("retryAfter")).longValue() > 0);
}

    @Test
public void testSuccessfulLoginResetsFailedAttempts() {
    // Arrange
    Map<String, String> invalidCredentials = new HashMap<>();
    invalidCredentials.put("username", "andres");
    invalidCredentials.put("password", "wrongPassword");

    Map<String, String> validCredentials = new HashMap<>();
    validCredentials.put("username", "andres");
    validCredentials.put("password", "1234");
    
    // Eliminados los stubbings innecesarios

    // Act - 2 failed login attempts
    loginController.login(invalidCredentials, mockRequest);
    loginController.login(invalidCredentials, mockRequest);
    
    // Successful login
    ResponseEntity<?> successResponse = loginController.login(validCredentials, mockRequest);
    
    // Try failed login again
    ResponseEntity<?> failedResponse = loginController.login(invalidCredentials, mockRequest);

    // Assert
    assertEquals(HttpStatus.OK, successResponse.getStatusCode());
    assertEquals(HttpStatus.UNAUTHORIZED, failedResponse.getStatusCode());
    assertEquals("Credenciales inválidas", failedResponse.getBody());
}

    @Test
    public void testAdminLogin() {
        // Arrange
        userCredentials.put("username", "diegot");
        userCredentials.put("password", "adminpass");

        // Act
        ResponseEntity<?> response = loginController.login(userCredentials, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        String token = responseBody.get("token");
        assertNotNull(token);
        // Verificar que el token tenga el formato correcto (podríamos decodificarlo para verificar el rol)
        assertTrue(token.split("\\.").length == 3); // formato JWT: header.payload.signature
    }
}
package juego.arsw;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import juego.arsw.controller.HealthyController;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(HealthyController.class)
public class HealthyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHealthCheckEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
    
    @Test
    public void testHealthCheckReturnsCorrectResponseEntity() {
        // Arrange
        HealthyController controller = new HealthyController();
        
        // Act
        var response = controller.healthCheck();
        
        // Assert
        assert response.getStatusCode().is2xxSuccessful();
        assert "OK".equals(response.getBody());
    }
}
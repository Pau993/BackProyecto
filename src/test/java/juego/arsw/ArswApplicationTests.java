package juego.arsw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ArswApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se carga correctamente
        assertNotNull(applicationContext);
    }

    @Test
    void mainMethodShouldStartWithoutExceptions() {
        // Simular llamada al método main
        // Este test básicamente verifica que no se lanzan excepciones
        ArswApplication.main(new String[]{});
    }
}
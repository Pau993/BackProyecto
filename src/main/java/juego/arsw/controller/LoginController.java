package juego.arsw.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class LoginController {

    // Define una clave secreta larga de mínimo 64 caracteres (512 bits)
    private static final String SECRET_STRING = "ClaveSuperLargaConMasDeSesentaCuatroCaracteresParaHS512_1234567890!@#$%^&*()";

    // Genera la clave segura a partir del string
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1 hora en milisegundos

    // Usuarios "quemados" para ejemplo
    private static final Map<String, String> users = new HashMap<>() {{
        put("andres", "1234");
        put("manuel", "abcd");
        put("paula", "pass123");
    }};

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Usuario y contraseña requeridos");
        }

        if (!users.containsKey(username) || !users.get(username).equals(password)) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }

        String token = Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();

        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }
}

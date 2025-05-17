package juego.arsw.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final String SECRET_STRING = "ClaveSuperLargaConMasDeSesentaCuatroCaracteresParaHS512_1234567890!@#$%^&*()";

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1 hora en milisegundos
    private final Map<String, FailedLoginAttempt> failedLoginAttempts = new ConcurrentHashMap<>();

    // Usuarios "quemados" para ejemplo
    private static final Map<String, String> users = new HashMap<>() {
        {
            put("andres", "1234");
            put("manuel", "abcd");
            put("paula", "pass123");
            put("diegot", "adminpass");
        }
    };

    private static final Map<String, String> roles = new HashMap<>() {
        {
            put("andres", "user");
            put("manuel", "user");
            put("paula", "user");
            put("diegot", "admin");
        }
    };

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> user, HttpServletRequest request) {
        String username = user.get("username");
        String password = user.get("password");

        log.info("Login attempt - user: {}", username);

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Usuario y contraseña requeridos");
        }

        if (isBlocked(username)) {
            long userBlockTime = getRemainingBlockTime(username);
            long remainingMillis = Math.max(userBlockTime,0);

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Demasiados intentos fallidos. Intente más tarde.");
            errorBody.put("retryAfter", remainingMillis / 1000); // en segundos
            errorBody.put("attemptsLeft", 0); // opcional si quieres

            return ResponseEntity.status(429).body(errorBody);
        }

        if (!users.containsKey(username) || !users.get(username).equals(password)) {
            registerFailedAttempt(username);
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }

        resetFailedAttempts(username);

        String role = roles.get(username);

        String token = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();

        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private boolean isBlocked(String username) {
        return username != null && isBlockedKey(username);
    }

    private boolean isBlockedKey(String key) {
        FailedLoginAttempt attempt = failedLoginAttempts.get(key);
        if (attempt == null)
            return false;
        if (attempt.getCount() >= 3) {
            long diff = System.currentTimeMillis() - attempt.getLastAttempt();
            if (diff < 60 * 1000) { // bloqueo de 1 minuto
                return true;
            } else {
                // Desbloquea si ya pasó tiempo
                failedLoginAttempts.remove(key);
                return false;
            }
        }
        return false;
    }

    private long getRemainingBlockTime(String key) {
        FailedLoginAttempt attempt = failedLoginAttempts.get(key);
        if (attempt == null)
            return 0;
        if (attempt.getCount() >= 3) {
            long diff = System.currentTimeMillis() - attempt.getLastAttempt();
            long blockDuration = 60 * 1000; // 1 minuto
            if (diff < blockDuration) {
                return blockDuration - diff; // tiempo restante en ms
            } else {
                failedLoginAttempts.remove(key);
                return 0;
            }
        }
        return 0;
    }

    private void registerFailedAttempt(String username) {
        if (username != null)
            registerFailedAttemptKey(username);
    }

    private void registerFailedAttemptKey(String key) {
        failedLoginAttempts.compute(key, (k, attempt) -> {
            if (attempt == null) {
                return new FailedLoginAttempt(1, System.currentTimeMillis());
            } else {
                attempt.increment();
                attempt.setLastAttempt(System.currentTimeMillis());
                return attempt;
            }
        });
    }

    private void resetFailedAttempts(String username) {
        if (username != null)
            failedLoginAttempts.remove(username);
    }

    private static class FailedLoginAttempt {
        private int count;
        private long lastAttempt;

        public FailedLoginAttempt(int count, long lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }

        public int getCount() {
            return count;
        }

        public long getLastAttempt() {
            return lastAttempt;
        }

        public void increment() {
            this.count++;
        }

        public void setLastAttempt(long lastAttempt) {
            this.lastAttempt = lastAttempt;
        }
    }
}

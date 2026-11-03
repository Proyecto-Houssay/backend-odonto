package com.proyectohoussay.odonto.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtTokenService {
    private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final long LIFETIME_SECONDS = 3600;

    private final ObjectMapper objectMapper;

    @Value("${app.auth.jwt-secret:}")
    private String secret;

    public JwtTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String issue(String username, String role) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET debe tener al menos 32 bytes");
        }
        long now = Instant.now().getEpochSecond();
        try {
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(
                    Map.of("sub", username, "role", role, "iat", now, "exp", now + LIFETIME_SECONDS)));
            String unsigned = HEADER + "." + payload;
            return unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(unsigned));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo emitir la sesión", exception);
        }
    }

    public String roleFrom(String token) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32 || token == null) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !HEADER.equals(parts[0])) return null;
            String unsigned = parts[0] + "." + parts[1];
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(unsigned), suppliedSignature)) return null;
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!claims.hasNonNull("sub") || !claims.hasNonNull("role") || !claims.hasNonNull("exp")) return null;
            if (claims.get("exp").asLong() <= Instant.now().getEpochSecond()) return null;
            return claims.get("role").asText();
        } catch (Exception ignored) {
            return null;
        }
    }

    public long lifetimeSeconds() {
        return LIFETIME_SECONDS;
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
}

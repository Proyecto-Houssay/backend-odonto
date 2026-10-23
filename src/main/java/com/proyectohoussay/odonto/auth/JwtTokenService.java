package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtTokenService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    @Autowired
    public JwtTokenService(JwtEncoder jwtEncoder) {
        this(jwtEncoder, Clock.systemUTC());
    }

    JwtTokenService(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    public AuthResponse issueToken(Usuario usuario) {
        UserRole role = UserRole.from(usuario.getRol())
                .orElseThrow(() -> new IllegalArgumentException("El usuario tiene un rol no válido."));
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(TOKEN_TTL);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(usuario.getUsername())
                .claim("userId", usuario.getId())
                .claim("role", role.name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", TOKEN_TTL.toSeconds());
    }
}

package com.toghrakx.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenService {

    private final SecretKey key;
    private final Long jwtExpirationMs;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") Long jwtExpirationMs) {
        // Convert the secret string to a Key.
        // The secret must be long enough (at least 64 characters for HS512).
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not set. Provide JWT_SECRET env/property and restart.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 64 bytes (512 bits) for HS512. " +
                    "Current length: " + keyBytes.length + " bytes. Provide a longer secret and restart.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /** Used at login — role is extracted directly from the authenticated principal. */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        // Strip the "ROLE_" prefix that Spring Security prepends
        String role = userPrincipal.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");

        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId()))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /** Used at token refresh — caller must supply the user's current role. */
    public String generateTokenForUser(Long userId, String roleName) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", roleName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /** Extracts the role name (without ROLE_ prefix) embedded in the token claim. */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Anyone can send a fake, expired or empty token, so this is routine, not an error.
            // Logged once, at DEBUG, so bad tokens cannot flood the logs; the request just stays anonymous.
            log.debug("Rejected JWT: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

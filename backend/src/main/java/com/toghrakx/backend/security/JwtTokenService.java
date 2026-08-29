package com.toghrakx.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenService {

    private final Key key;
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
                .setSubject(String.valueOf(userPrincipal.getId()))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /** Used at token refresh — caller must supply the user's current role. */
    public String generateTokenForUser(Long userId, String roleName) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", roleName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
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
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

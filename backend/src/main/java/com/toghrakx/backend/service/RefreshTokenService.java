package com.toghrakx.backend.service;

import com.toghrakx.backend.exception.ResourceNotFoundException;
import com.toghrakx.backend.model.RefreshToken;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.RefreshTokenRepository;
import com.toghrakx.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTtlMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.refresh.expiration:43200000}") long refreshTtlMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTtlMs = refreshTtlMs;
    }

    public RefreshToken create(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String raw = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .token(hash(raw))
                .user(user)
                .expiresAt(Instant.now().plus(refreshTtlMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
        token.setPlainToken(raw);
        return refreshTokenRepository.save(token);
    }

    // noRollbackFor: when a revoked token is reused, the "revoke all" below must be saved even though
    // the request then fails with 401.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public RefreshToken rotate(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(hash(tokenValue))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));
        if (existing.isRevoked()) {
            // A token that was already rotated or logged out is being used again: two parties hold a copy,
            // so it may have been stolen. Revoke all of the user's tokens so everyone must log in again
            // (refresh token reuse detection, OAuth 2.0 Security Best Current Practice, RFC 9700).
            refreshTokenRepository.revokeAllByUserId(existing.getUser().getId());
            throw new BadCredentialsException("Refresh token has been revoked.");
        }
        validate(existing);
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
        return create(existing.getUser().getId());
    }

    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(hash(tokenValue)).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Cleanup job to purge expired or revoked refresh tokens.
     * Intended to be called on a schedule (e.g., nightly).
     */
    public int cleanupExpiredOrRevoked() {
        return refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
    }

    // Run nightly at 03:00 server time
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledCleanup() {
        cleanupExpiredOrRevoked();
    }

    private void validate(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
            // Expired tokens are an auth failure — client must re-authenticate.
            throw new BadCredentialsException("Refresh token has expired.");
        }
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

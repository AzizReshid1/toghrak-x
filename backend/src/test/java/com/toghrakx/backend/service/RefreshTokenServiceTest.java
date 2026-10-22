package com.toghrakx.backend.service;

import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.model.RefreshToken;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Not @Transactional on purpose: every service call commits, as in production. That is the only way
// to prove the "revoke all" on token reuse survives the 401 instead of being rolled back.
@IntegrationTest
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    void createUser() {
        user = userService.createUser("refresh-user", "refresh-user@example.com", "password", "EDITOR");
    }

    @AfterEach
    void deleteUser() {
        userService.deleteUser(user.getId()); // also deletes the user's refresh tokens
    }

    @Test
    void unknownTokenIsAnAuthenticationFailure() {
        assertThatThrownBy(() -> refreshTokenService.rotate("not-a-real-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void reusingARotatedTokenRevokesAllOfTheUsersTokens() {
        RefreshToken original = refreshTokenService.create(user.getId());
        RefreshToken rotated = refreshTokenService.rotate(original.getPlainToken());

        // Someone presents the old token again (e.g. an attacker who copied it)
        assertThatThrownBy(() -> refreshTokenService.rotate(original.getPlainToken()))
                .isInstanceOf(BadCredentialsException.class);

        // The newest token is revoked too, so the real user and the attacker must both log in again
        assertThat(refreshTokenRepository.findById(rotated.getId()).orElseThrow().isRevoked()).isTrue();
    }
}

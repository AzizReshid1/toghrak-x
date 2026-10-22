package com.toghrakx.backend.service;

import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.dto.UpdateUserRequest;
import com.toghrakx.backend.model.RefreshToken;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.RefreshTokenRepository;
import com.toghrakx.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Each test runs in a transaction that is rolled back afterwards, so tests do not affect each other.
@IntegrationTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void deleteUser_removesUserWhoHasLoggedIn() {
        userService.createUser("admin1", "admin1@example.com", "password", "ADMIN");
        User editor = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        refreshTokenService.create(editor.getId()); // what every login does

        userService.deleteUser(editor.getId());
        userRepository.flush(); // force the DELETE now so a foreign key violation would surface here

        assertThat(userRepository.findById(editor.getId())).isEmpty();
    }

    @Test
    void deleteUser_refusesToDeleteLastAdmin() {
        User admin = userService.createUser("admin1", "admin1@example.com", "password", "ADMIN");

        assertThatThrownBy(() -> userService.deleteUser(admin.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void updateUser_refusesToDemoteLastAdmin() {
        User admin = userService.createUser("admin1", "admin1@example.com", "password", "ADMIN");

        assertThatThrownBy(() -> userService.updateUser(admin.getId(), admin.getId(), true, editorRole()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    void updateUser_allowsDemotingAdminWhenAnotherAdminExists() {
        User admin = userService.createUser("admin1", "admin1@example.com", "password", "ADMIN");
        userService.createUser("admin2", "admin2@example.com", "password", "ADMIN");

        User updated = userService.updateUser(admin.getId(), admin.getId(), true, editorRole());

        assertThat(updated.getRole().getName()).isEqualTo("EDITOR");
    }

    @Test
    void updateUser_passwordChangeRevokesRefreshTokens() {
        User editor = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        RefreshToken token = refreshTokenService.create(editor.getId());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("new-password");
        userService.updateUser(editor.getId(), editor.getId(), false, request);

        assertThat(isRevoked(token)).isTrue();
    }

    @Test
    void updateUser_roleChangeRevokesRefreshTokens() {
        userService.createUser("admin1", "admin1@example.com", "password", "ADMIN");
        User admin2 = userService.createUser("admin2", "admin2@example.com", "password", "ADMIN");
        RefreshToken token = refreshTokenService.create(admin2.getId());

        userService.updateUser(admin2.getId(), admin2.getId(), true, editorRole());

        assertThat(isRevoked(token)).isTrue();
    }

    @Test
    void updateUser_emailChangeKeepsRefreshTokens() {
        User editor = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        RefreshToken token = refreshTokenService.create(editor.getId());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new-address@example.com");
        userService.updateUser(editor.getId(), editor.getId(), false, request);

        assertThat(isRevoked(token)).isFalse();
    }

    private boolean isRevoked(RefreshToken token) {
        refreshTokenRepository.flush();
        return refreshTokenRepository.findById(token.getId()).orElseThrow().isRevoked();
    }

    private static UpdateUserRequest editorRole() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRoleName("EDITOR");
        return request;
    }
}

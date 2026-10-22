package com.toghrakx.backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void isAdminOnlyForTheAdminRole() {
        assertThat(UserPrincipal.fromToken(1L, "ADMIN").isAdmin()).isTrue();
        assertThat(UserPrincipal.fromToken(2L, "EDITOR").isAdmin()).isFalse();
    }
}

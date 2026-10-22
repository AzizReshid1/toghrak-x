package com.toghrakx.backend.config;

import com.toghrakx.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Security headers come from Spring Security's headers() configuration, each exactly once.
@IntegrationTest
@AutoConfigureMockMvc
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void responsesCarryTheSecurityHeadersOnce() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/posts")).andReturn().getResponse();

        assertThat(response.getHeaders("X-Content-Type-Options")).containsExactly("nosniff");
        assertThat(response.getHeaders("X-Frame-Options")).containsExactly("DENY");
        assertThat(response.getHeaders("X-XSS-Protection")).containsExactly("0");
        assertThat(response.getHeaders("Referrer-Policy")).containsExactly("strict-origin-when-cross-origin");
        assertThat(response.getHeaders("Content-Security-Policy")).hasSize(1);
        assertThat(response.getHeader("Content-Security-Policy")).startsWith("default-src 'self'");
    }
}

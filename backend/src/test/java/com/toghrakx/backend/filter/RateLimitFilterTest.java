package com.toghrakx.backend.filter;

import com.toghrakx.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Each test uses its own client IPs, so other tests' requests do not use up these buckets.
@IntegrationTest
@AutoConfigureMockMvc
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void eleventhLoginAttemptInAMinuteIsRejected() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(login("10.0.0.1")).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(login("10.0.0.1")).andExpect(status().isTooManyRequests());
    }

    @Test
    void limitsArePerClientIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(login("10.0.0.2"));
        }
        mockMvc.perform(login("10.0.0.2")).andExpect(status().isTooManyRequests());
        // Another client is not affected
        mockMvc.perform(login("10.0.0.3")).andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder login(String clientIp) {
        return post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobody\",\"password\":\"wrong-password\"}");
    }
}

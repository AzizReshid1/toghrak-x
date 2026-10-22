package com.toghrakx.backend.config;

import com.toghrakx.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks both access layers together: URL rules (public vs logged in) and @PreAuthorize (roles).
// Requests carry valid bodies so the answer comes from security, not from validation.
@IntegrationTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicContentIsReadableWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/posts")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void writingRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"content\":\"c\",\"categoryId\":1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/pages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"about\",\"title\":\"About\",\"content\":\"c\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"News\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/site-settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"slogan\":\"s\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
    }
}

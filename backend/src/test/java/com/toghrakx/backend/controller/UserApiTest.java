package com.toghrakx.backend.controller;

import com.toghrakx.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class UserApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createUserRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(newUser("a")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserAcceptsPasswordOfEightCharacters() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(newUser("12345678")))
                .andExpect(status().isCreated());
    }

    @Test
    void dashboardReturnsCountsWithTheSameFieldNames() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalAdmins").isNumber())
                .andExpect(jsonPath("$.totalEditors").isNumber())
                .andExpect(jsonPath("$.totalPosts").isNumber());
    }

    private static String newUser(String password) {
        return "{\"username\":\"editor1\",\"email\":\"editor1@example.com\",\"password\":\"" + password
                + "\",\"roleName\":\"EDITOR\"}";
    }
}

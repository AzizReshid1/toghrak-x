package com.toghrakx.backend.exception;

import com.toghrakx.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every error, wherever it is raised, must use the same RFC 9457 ProblemDetail shape.
@IntegrationTest
@AutoConfigureMockMvc
class ErrorFormatTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthorizedFromSecurityRules() throws Exception {
        expectProblem(mockMvc.perform(get("/api/users")), 401);
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void forbiddenFromMethodSecurity() throws Exception {
        expectProblem(mockMvc.perform(get("/api/users")), 403);
    }

    @Test
    void notFoundFromService() throws Exception {
        expectProblem(mockMvc.perform(get("/api/posts/slug/does-not-exist")), 404)
                .andExpect(jsonPath("$.detail").value("Post not found: does-not-exist"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validationErrorsListEachField() throws Exception {
        expectProblem(mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")), 400)
                .andExpect(jsonPath("$.errors.name").value("Category name is required"));
    }

    @Test
    void tooManyRequestsFromRateLimiter() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/search").param("q", "test"));
        }
        expectProblem(mockMvc.perform(get("/api/search").param("q", "test")), 429);
    }

    private static ResultActions expectProblem(ResultActions result, int status) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").isString());
    }
}

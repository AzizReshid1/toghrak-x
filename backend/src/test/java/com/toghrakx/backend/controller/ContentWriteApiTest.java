package com.toghrakx.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.CategoryRepository;
import com.toghrakx.backend.repository.PageRepository;
import com.toghrakx.backend.repository.PostRepository;
import com.toghrakx.backend.repository.RefreshTokenRepository;
import com.toghrakx.backend.repository.UserRepository;
import com.toghrakx.backend.service.CategoryService;
import com.toghrakx.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Not @Transactional on purpose: responses are built after the service transaction has ended,
// exactly as in production, so anything not loaded in time would fail here (LazyInitializationException).
@IntegrationTest
@AutoConfigureMockMvc
class ContentWriteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User admin;
    private Category category;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        admin = userService.createUser("write-admin", "write-admin@example.com", "password123", "ADMIN");
        category = categoryService.createCategory("Write tests", null);
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"write-admin\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + objectMapper.readTree(body).get("token").asText();
    }

    @AfterEach
    void tearDown() {
        // One transaction for the cleanup (the bulk refresh-token delete requires one)
        transactionTemplate.executeWithoutResult(status -> {
            postRepository.findByAuthorId(admin.getId()).forEach(postRepository::delete);
            pageRepository.findBySlug("write-test-page").ifPresent(pageRepository::delete);
            pageRepository.findBySlug("write-test-page-2").ifPresent(pageRepository::delete);
            categoryRepository.deleteById(category.getId());
            refreshTokenRepository.deleteAllByUserId(admin.getId());
            userRepository.deleteById(admin.getId());
        });
    }

    @Test
    void createAndUpdatePostReturnFullResponse() throws Exception {
        String post = "{\"title\":\"Hello\",\"content\":\"<p>Body</p>\",\"categoryId\":" + category.getId()
                + ",\"imageUrls\":[\"https://example.com/a.jpg\"]}";
        String created = mockMvc.perform(post("/api/posts").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(post))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(admin.getId()))
                .andExpect(jsonPath("$.authorUsername").value("write-admin"))
                .andExpect(jsonPath("$.categoryName").value("Write tests"))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/a.jpg"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/posts/" + id).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(post.replace("Hello", "Hello again")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello again"))
                .andExpect(jsonPath("$.authorUsername").value("write-admin"))
                .andExpect(jsonPath("$.categoryName").value("Write tests"))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/a.jpg"));
    }

    @Test
    void createAndUpdatePageReturnFullResponse() throws Exception {
        String page = "{\"slug\":\"write-test-page\",\"title\":\"About\",\"content\":\"<p>Body</p>\","
                + "\"images\":[\"https://example.com/p.jpg\"]}";
        mockMvc.perform(post("/api/pages").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(page))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images[0]").value("https://example.com/p.jpg"));

        mockMvc.perform(put("/api/pages/write-test-page").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(page.replace("\"write-test-page\"", "\"write-test-page-2\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("write-test-page-2"))
                .andExpect(jsonPath("$.images[0]").value("https://example.com/p.jpg"));
    }
}

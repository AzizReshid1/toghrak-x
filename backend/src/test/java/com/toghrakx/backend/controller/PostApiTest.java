package com.toghrakx.backend.controller;

import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.dto.CreatePostRequest;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.service.CategoryService;
import com.toghrakx.backend.service.PostService;
import com.toghrakx.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks the JSON contract the Angular app depends on.
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class PostApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostService postService;

    @Test
    void postListUsesStablePageShape() throws Exception {
        mockMvc.perform(get("/api/posts").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.totalPages").isNumber());
    }

    @Test
    void listsSendSummariesWithoutFullContent() throws Exception {
        User author = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        Category category = categoryService.createCategory("News", null);
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Hello");
        request.setContent("<p>Full <strong>article</strong> body</p>");
        request.setCategoryId(category.getId());
        postService.createPost(author.getId(), request);

        mockMvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.content[0].excerpt").value("Full article body"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist())
                .andExpect(jsonPath("$.content[0].imageUrls").doesNotExist());
        mockMvc.perform(get("/api/posts/popular"))
                .andExpect(jsonPath("$[0].excerpt").value("Full article body"))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }
}

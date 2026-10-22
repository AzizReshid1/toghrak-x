package com.toghrakx.backend.controller;

import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.dto.CreatePostRequest;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.Post;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.SiteSettingsRepository;
import com.toghrakx.backend.service.CategoryService;
import com.toghrakx.backend.service.PostService;
import com.toghrakx.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

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
    void readingAPostDoesNotCountAViewButRecordingOneDoes() throws Exception {
        Post post = createPost();

        mockMvc.perform(get("/api/posts/slug/" + post.getSlug())).andExpect(status().isOk());
        mockMvc.perform(get("/api/posts/slug/" + post.getSlug()))
                .andExpect(jsonPath("$.viewCount").value(0));

        // Anonymous readers may record a view
        mockMvc.perform(post("/api/posts/" + post.getId() + "/view")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/slug/" + post.getSlug()))
                .andExpect(jsonPath("$.viewCount").value(1));
    }

    @Test
    void readingSiteSettingsDoesNotCreateRows() throws Exception {
        long before = siteSettingsRepository.count();
        mockMvc.perform(get("/api/site-settings")).andExpect(status().isOk());
        assertThat(siteSettingsRepository.count()).isEqualTo(before);
    }

    @Test
    void listsSendSummariesWithoutFullContent() throws Exception {
        createPost();

        mockMvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.content[0].excerpt").value("Full article body"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist())
                .andExpect(jsonPath("$.content[0].imageUrls").doesNotExist());
        mockMvc.perform(get("/api/posts/popular"))
                .andExpect(jsonPath("$[0].excerpt").value("Full article body"))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void postWithUyghurTitleCanBeOpenedByItsSlug() throws Exception {
        Post post = createPost("ئۇيغۇر تىلى");

        assertThat(post.getSlug()).isEqualTo("ئۇيغۇر-تىلى");
        // The browser sends the slug percent-encoded, exactly like this
        String encoded = java.net.URLEncoder.encode(post.getSlug(), java.nio.charset.StandardCharsets.UTF_8);
        mockMvc.perform(get(java.net.URI.create("/api/posts/slug/" + encoded)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("ئۇيغۇر تىلى"));
    }

    private Post createPost() {
        return createPost("Hello");
    }

    private Post createPost(String title) {
        User author = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        Category category = categoryService.createCategory("News", null);
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(title);
        request.setContent("<p>Full <strong>article</strong> body</p>");
        request.setCategoryId(category.getId());
        return postService.createPost(author.getId(), request);
    }
}

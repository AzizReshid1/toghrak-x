package com.toghrakx.backend.service;

import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.dto.CreatePostRequest;
import com.toghrakx.backend.dto.UpdatePostRequest;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.Post;
import com.toghrakx.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Each test runs in a transaction that is rolled back afterwards, so tests do not affect each other.
@IntegrationTest
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void updatePost_keepsSlugWhenTitleChanges() {
        User author = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        Category category = categoryService.createCategory("News", null);
        Post post = postService.createPost(author.getId(), createRequest("Original title", category));

        UpdatePostRequest update = updateRequest("A completely new title", category);
        Post updated = postService.updatePost(post.getId(), author.getId(), false, update);

        assertThat(updated.getTitle()).isEqualTo("A completely new title");
        assertThat(updated.getSlug()).isEqualTo("original-title");
    }

    @Test
    void updatePost_replacesWholePostSoCoverAndGalleryCanBeRemoved() {
        User author = userService.createUser("editor1", "editor1@example.com", "password", "EDITOR");
        Category category = categoryService.createCategory("News", null);
        CreatePostRequest create = createRequest("Title", category);
        create.setCoverImage("http://localhost:8081/images/cover.jpg");
        create.setImageUrls(List.of("http://localhost:8081/images/a.jpg"));
        Post post = postService.createPost(author.getId(), create);

        // The edit form sends no cover image and an empty gallery after the user removes them
        Post updated = postService.updatePost(post.getId(), author.getId(), false, updateRequest("Title", category));

        assertThat(updated.getCoverImage()).isNull();
        assertThat(updated.getImages()).isEmpty();
    }

    private static UpdatePostRequest updateRequest(String title, Category category) {
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle(title);
        request.setContent("<p>Body</p>");
        request.setCategoryId(category.getId());
        return request;
    }

    private static CreatePostRequest createRequest(String title, Category category) {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(title);
        request.setContent("<p>Body</p>");
        request.setCategoryId(category.getId());
        return request;
    }
}

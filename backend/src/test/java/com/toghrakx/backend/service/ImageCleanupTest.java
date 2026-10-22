package com.toghrakx.backend.service;

import com.toghrakx.backend.IntegrationTest;
import com.toghrakx.backend.config.UploadPathResolver;
import com.toghrakx.backend.dto.CreatePostRequest;
import com.toghrakx.backend.dto.UpdatePostRequest;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.Post;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Not @Transactional: files are only deleted after a real commit, as in production.
// Uploads go to the temporary folder set in application-test.properties.
@IntegrationTest
class ImageCleanupTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Value("${app.image.upload-dir}")
    private String uploadDir;

    private User author;
    private Category category;

    @BeforeEach
    void setUp() {
        author = userService.createUser("image-editor", "image-editor@example.com", "password", "EDITOR");
        category = categoryService.createCategory("Image tests", null);
    }

    @AfterEach
    void tearDown() {
        // Delete any posts the test left behind, then the author and category
        postService.getPosts(org.springframework.data.domain.Pageable.unpaged(), category.getId())
                .forEach(p -> postService.deletePost(p.getId(), author.getId(), true));
        userService.deleteUser(author.getId());
        categoryRepository.deleteById(category.getId());
    }

    @Test
    void deletingAPostDeletesItsImages() throws IOException {
        String cover = upload();
        String gallery = upload();
        Post post = createPost(cover, List.of(gallery));

        postService.deletePost(post.getId(), author.getId(), false);

        assertThat(fileOf(cover)).doesNotExist();
        assertThat(fileOf(gallery)).doesNotExist();
    }

    @Test
    void imageStillUsedByAnotherPostIsKept() throws IOException {
        String shared = upload();
        Post first = createPost(shared, List.of());
        createPost(shared, List.of());

        postService.deletePost(first.getId(), author.getId(), false);

        assertThat(fileOf(shared)).exists();
    }

    @Test
    void removingTheCoverImageInAnUpdateDeletesTheFile() throws IOException {
        String cover = upload();
        Post post = createPost(cover, List.of());

        UpdatePostRequest update = new UpdatePostRequest();
        update.setTitle("Title");
        update.setContent("<p>Body</p>");
        update.setCategoryId(category.getId());
        postService.updatePost(post.getId(), author.getId(), false, update);

        assertThat(fileOf(cover)).doesNotExist();
    }

    private Post createPost(String cover, List<String> gallery) {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Title");
        request.setContent("<p>Body</p>");
        request.setCategoryId(category.getId());
        request.setCoverImage(cover);
        request.setImageUrls(gallery);
        return postService.createPost(author.getId(), request);
    }

    private String upload() throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", png);
        String url = imageService.storeFile(new MockMultipartFile("file", "test.png", "image/png", png.toByteArray()));
        assertThat(fileOf(url)).exists();
        return url;
    }

    private Path fileOf(String url) {
        return UploadPathResolver.resolve(uploadDir).resolve(url.substring(url.lastIndexOf('/') + 1));
    }
}

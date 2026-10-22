package com.toghrakx.backend.service;

import com.toghrakx.backend.dto.CreatePostRequest;
import com.toghrakx.backend.dto.UpdatePostRequest;
import com.toghrakx.backend.exception.ResourceNotFoundException;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.Post;
import com.toghrakx.backend.model.PostImage;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.CategoryRepository;
import com.toghrakx.backend.repository.PostRepository;
import com.toghrakx.backend.repository.UserRepository;
import com.toghrakx.backend.service.support.UserContentSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserContentSanitizer sanitizer;

    public PostService(PostRepository postRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            UserContentSanitizer sanitizer) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.sanitizer = sanitizer;
    }

    public Post createPost(Long authorId, CreatePostRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id=" + authorId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category not found with id=" + request.getCategoryId()));

        String slug = generateUniqueSlug(request.getTitle());

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(slug)
                .content(sanitizer.sanitize(request.getContent()))
                .author(author)
                .category(category)
                .coverImage(request.getCoverImage())
                .build(); // createdAt is set by @PrePersist on Post

        setImages(post, request.getImageUrls());

        Post saved = postRepository.save(post);
        return postRepository.findWithImagesById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + saved.getId()));
    }

    // 2️⃣ Update an existing post
    public Post updatePost(Long postId, Long requestUserId, boolean requestUserAdmin, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        User author = post.getAuthor();
        if (author == null) {
            throw new ResourceNotFoundException("Post author missing for post: " + postId);
        }
        if (!requestUserAdmin && !author.getId().equals(requestUserId)) {
            throw new AccessDeniedException("You can only update your own posts!");
        }

        String newTitle = request.getTitle();
        String newContent = request.getContent();
        Long newCategoryId = request.getCategoryId();
        String newCoverImage = request.getCoverImage();
        List<String> imageUrls = request.getImageUrls();

        // The slug is set once at creation and never changes, so shared links and search results keep working.
        if (newTitle != null && !newTitle.isBlank()) {
            post.setTitle(newTitle);
        }
        if (newContent != null && !newContent.isBlank()) {
            post.setContent(sanitizer.sanitize(newContent));
        }
        if (newCategoryId != null) {
            Category category = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + newCategoryId));
            post.setCategory(category);
        }
        if (newCoverImage != null && !newCoverImage.isBlank()) {
            post.setCoverImage(newCoverImage);
        }

        if (imageUrls != null) {
            setImages(post, imageUrls);
        }

        // updatedAt is set automatically by @PreUpdate on Post — no manual assignment needed.
        Post saved = postRepository.save(post);
        return postRepository.findWithImagesById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + saved.getId()));
    }

    // 3️⃣ Delete a post
    // Clean-architecture overload: authorization decided at controller/security
    // layer.
    public void deletePost(Long postId, Long requestUserId, boolean requestUserAdmin) {
        // Always verify the post exists first — consistent 404 for both admin and non-admin paths.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete. Post not found: " + postId));

        if (!requestUserAdmin) {
            User author = post.getAuthor();
            if (author == null) {
                throw new ResourceNotFoundException("Post author missing for post: " + postId);
            }
            if (!author.getId().equals(requestUserId)) {
                throw new AccessDeniedException("You can only delete your own posts!");
            }
        }

        postRepository.deleteById(postId);
    }

    // 4️⃣ Retrieve a single post by slug (SEO-friendly URL) — read-only; views are recorded by recordView()
    @Transactional(readOnly = true)
    public Post getPostBySlug(String slug) {
        Long id = postRepository.findIdBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));
        return getPost(id);
    }

    // Counts one view. A separate POST so that GET stays safe: refreshes of the API, crawlers
    // and link prefetching do not change data.
    public void recordView(Long postId) {
        if (postRepository.incrementViewCount(postId) == 0) {
            throw new ResourceNotFoundException("Post not found: " + postId);
        }
    }

    // Retrieve a single post by numeric ID (edit form, admin) — read-only, does not count a view
    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findWithImagesById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
    }

    // 5️⃣ List all posts (paginated). Lists are shown as summaries, so gallery images are not loaded.
    @Transactional(readOnly = true)
    public Page<Post> getPosts(Pageable pageable, Long categoryId) {
        if (categoryId != null) {
            return postRepository.findByCategoryId(categoryId, pageable);
        }
        return postRepository.findAllBy(pageable);
    }

    @Transactional(readOnly = true)
    public List<Post> getPopular(int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        return postRepository
                .findAllByOrderByViewCountDescUpdatedAtDescCreatedAtDesc(PageRequest.of(0, size));
    }

    private String generateUniqueSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int counter = 1;
        while (postRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "post-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "post-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

    private void setImages(Post post, List<String> imageUrls) {
        post.getImages().clear();
        if (imageUrls == null) {
            return;
        }
        List<PostImage> newImages = new ArrayList<>();
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            newImages.add(PostImage.builder()
                    .imageUrl(url)
                    .post(post)
                    .build());
        }
        post.getImages().addAll(newImages);
    }

    // Sanitization handled by UserContentSanitizer component.
}

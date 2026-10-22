package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.CreatePostRequest;
import com.toghrakx.backend.dto.PostResponse;
import com.toghrakx.backend.dto.PostSummary;
import com.toghrakx.backend.dto.UpdatePostRequest;
import com.toghrakx.backend.model.Post;
import com.toghrakx.backend.model.PostImage;
import com.toghrakx.backend.security.UserPrincipal;
import com.toghrakx.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@Slf4j
public class PostController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "viewCount", "title");

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 1) CREATE a new post
    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Post newPost = postService.createPost(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPostResponse(newPost));
    }

    // paginated GET /api/posts
    @GetMapping
    public ResponseEntity<Page<PostSummary>> getPosts(
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        // hard cap page size to prevent abuse (e.g., max 50)
        int safeSize = Math.min(pageable.getPageSize(), 50);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), safeSize, safeSort(pageable.getSort()));

        Page<PostSummary> page = postService
                .getPosts(safePageable, categoryId)
                .map(this::toPostSummary);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PostSummary>> getPopular(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        var popular = postService.getPopular(limit).stream()
                .map(this::toPostSummary)
                .toList();
        return ResponseEntity.ok(popular);
    }

    // 3a) GET single post by slug — used by the public-facing frontend (SEO-friendly URLs)
    // Must be declared before /{id} so Spring MVC matches the literal "slug" segment first.
    @GetMapping("/slug/{slug}")
    public ResponseEntity<PostResponse> getPostBySlug(@PathVariable String slug) {
        Post post = postService.getPostBySlug(slug);
        return ResponseEntity.ok(toPostResponse(post));
    }

    // 3c) Record one view of a post — called by the post page after it loads. Public, like reading.
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id) {
        postService.recordView(id);
        return ResponseEntity.noContent().build();
    }

    // 3b) GET single post by numeric ID — kept for internal use (edit form, admin)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(toPostResponse(post));
    }

    // 4) UPDATE a post (Only the owner can update)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Post updatedPost = postService.updatePost(id, currentUser.getId(), currentUser.isAdmin(), request);
        return ResponseEntity.ok(toPostResponse(updatedPost));
    }

    // 5) DELETE a post (Only the owner or admin can delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.deletePost(id, currentUser.getId(), currentUser.isAdmin());
        return ResponseEntity.noContent().build();
    }

    /**
     * Keeps only whitelisted sort fields. Without this, ?sort=abc causes a 500 error and
     * ?sort=author.password lets anyone order posts by their authors' password hashes.
     */
    private static Sort safeSort(Sort requested) {
        List<Sort.Order> allowed = requested.stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList();
        return allowed.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(allowed);
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setContent(post.getContent());
        response.setContentType("html");
        response.setExcerpt(buildExcerpt(post.getContentText(), 200));
        response.setCoverImage(post.getCoverImage());
        response.setCategoryId(post.getCategory().getId());
        response.setCategoryName(post.getCategory().getName());
        response.setAuthorUsername(post.getAuthor().getUsername());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        response.setViewCount(post.getViewCount());
        response.setImageUrls(
                post.getImages().stream()
                        .map(PostImage::getImageUrl)
                        .toList());
        return response;
    }

    // List/card view: no HTML content or gallery, so responses stay small and no images are loaded.
    private PostSummary toPostSummary(Post post) {
        PostSummary summary = new PostSummary();
        summary.setId(post.getId());
        summary.setTitle(post.getTitle());
        summary.setSlug(post.getSlug());
        summary.setExcerpt(buildExcerpt(post.getContentText(), 200));
        summary.setCoverImage(post.getCoverImage());
        summary.setCategoryId(post.getCategory().getId());
        summary.setCategoryName(post.getCategory().getName());
        summary.setAuthorUsername(post.getAuthor().getUsername());
        summary.setCreatedAt(post.getCreatedAt());
        summary.setUpdatedAt(post.getUpdatedAt());
        summary.setViewCount(post.getViewCount());
        return summary;
    }

    /** Trims the post's plain-text copy (HTML already stripped on save) to {@code maxLen} characters. */
    private static String buildExcerpt(String text, int maxLen) {
        if (text == null || text.isBlank()) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen).trim() + "\u2026";
    }

}

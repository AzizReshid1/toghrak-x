package com.toghrak.backend.controller;

import com.toghrak.backend.dto.CreatePostRequest;
import com.toghrak.backend.dto.PostResponse;
import com.toghrak.backend.dto.UpdatePostRequest;
import com.toghrak.backend.model.Post;
import com.toghrak.backend.model.PostImage;
import com.toghrak.backend.security.UserPrincipal;
import com.toghrak.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@Slf4j
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 1) CREATE a new post
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Post newPost = postService.createPost(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPostResponse(newPost));
    }

    // paginated GET /api/posts
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        // hard cap page size to prevent abuse (e.g., max 50)
        int safeSize = Math.min(pageable.getPageSize(), 50);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), safeSize, pageable.getSort());

        Page<PostResponse> page = postService
                .getPosts(safePageable, categoryId)
                .map(this::toPostResponse);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/popular")
    public ResponseEntity<java.util.List<PostResponse>> getPopular(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        var popular = postService.getPopular(limit).stream()
                .map(this::toPostResponse)
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

    // 3b) GET single post by numeric ID — kept for internal use (edit form, admin)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(toPostResponse(post));
    }

    // 4) UPDATE a post (Only the owner can update)
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Post updatedPost = postService.updatePost(id, currentUser.getId(), isAdmin(currentUser), request);
        return ResponseEntity.ok(toPostResponse(updatedPost));
    }

    // 5) DELETE a post (Only the owner or admin can delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.deletePost(id, currentUser.getId(), isAdmin(currentUser));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(UserPrincipal currentUser) {
        return currentUser != null && currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setContent(post.getContent());
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

}

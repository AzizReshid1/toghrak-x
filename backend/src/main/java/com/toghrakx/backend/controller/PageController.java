package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.PageResponse;
import com.toghrakx.backend.dto.UpsertPageRequest;
import com.toghrakx.backend.model.Page;
import com.toghrakx.backend.service.PageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pages")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PageResponse> getBySlug(@PathVariable String slug) {
        Page page = pageService.getBySlug(slug);
        return ResponseEntity.ok(toResponse(page));
    }

    @GetMapping
    public List<PageResponse> list() {
        return pageService.list().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse> create(@Valid @RequestBody UpsertPageRequest request) {
        Page created = pageService.create(
                request.getSlug(),
                request.getTitle(),
                request.getContent(),
                request.getImages());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse> update(@PathVariable String slug,
            @Valid @RequestBody UpsertPageRequest request) {
        Page updated = pageService.update(
                slug,
                request.getSlug(),
                request.getTitle(),
                request.getContent(),
                request.getImages());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        pageService.delete(slug);
        return ResponseEntity.noContent().build();
    }

    private PageResponse toResponse(Page page) {
        PageResponse res = new PageResponse();
        res.setSlug(page.getSlug());
        res.setTitle(page.getTitle());
        res.setContent(page.getContent());
        res.setUpdatedAt(page.getUpdatedAt());
        res.setImages(page.getImages());
        return res;
    }
}

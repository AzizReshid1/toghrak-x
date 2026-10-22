package com.toghrakx.backend.service;

import com.toghrakx.backend.service.support.SlugUtil;
import com.toghrakx.backend.exception.ResourceNotFoundException;
import com.toghrakx.backend.model.Page;
import com.toghrakx.backend.repository.PageRepository;
import com.toghrakx.backend.service.support.UserContentSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

@Service
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final UserContentSanitizer sanitizer;

    public PageService(PageRepository pageRepository, UserContentSanitizer sanitizer) {
        this.pageRepository = pageRepository;
        this.sanitizer = sanitizer;
    }

    public Page create(String slug, String title, String content, List<String> images) {
        String normalizedSlug = normalizeSlug(slug);
        if (pageRepository.existsBySlug(normalizedSlug)) {
            throw new IllegalStateException("Page slug already exists: " + normalizedSlug);
        }
        Page page = Page.builder()
                .slug(normalizedSlug)
                .title(title)
                .content(sanitizer.sanitize(content))
                .images(cleanImages(images))
                .build();
        return pageRepository.save(page);
    }

    public Page update(String slug, String newSlug, String title, String content, List<String> images) {
        Page existing = pageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + slug));
        String normalizedSlug = normalizeSlug(newSlug);
        if (!normalizedSlug.equals(existing.getSlug()) && pageRepository.existsBySlug(normalizedSlug)) {
            throw new IllegalStateException("Page slug already exists: " + normalizedSlug);
        }
        existing.setSlug(normalizedSlug);
        existing.setTitle(title);
        existing.setContent(sanitizer.sanitize(content));
        existing.setImages(cleanImages(images));
        // findBySlug above already loaded the images, so the saved page can be returned as is
        return pageRepository.save(existing);
    }

    public void delete(String slug) {
        Page existing = pageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + slug));
        pageRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public Page getBySlug(String slug) {
        return pageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + slug));
    }

    @Transactional(readOnly = true)
    public List<Page> list() {
        // Fetch images explicitly to avoid LazyInitializationException in controllers
        return pageRepository.findAllByOrderByUpdatedAtDesc();
    }

    // Sanitization handled by UserContentSanitizer component.

    private List<String> cleanImages(List<String> images) {
        if (images == null)
            return new ArrayList<>();
        var list = images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return new ArrayList<>(list);
    }

    private String normalizeSlug(String slug) {
        if (slug == null) {
            throw new IllegalArgumentException("Slug is required");
        }
        String normalized = SlugUtil.slugify(slug, 100);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Slug is invalid");
        }
        return normalized;
    }
}

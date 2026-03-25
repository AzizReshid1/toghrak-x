package com.toghrak.backend.service;

import com.toghrak.backend.model.Page;
import com.toghrak.backend.model.Post;
import com.toghrak.backend.model.SocialLink;
import com.toghrak.backend.dto.SearchResultItem;
import com.toghrak.backend.repository.PageRepository;
import com.toghrak.backend.repository.PostRepository;
import com.toghrak.backend.repository.SocialLinkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final PostRepository postRepository;
    private final PageRepository pageRepository;
    private final SocialLinkRepository socialLinkRepository;

    public SearchService(PostRepository postRepository, PageRepository pageRepository,
            SocialLinkRepository socialLinkRepository) {
        this.postRepository = postRepository;
        this.pageRepository = pageRepository;
        this.socialLinkRepository = socialLinkRepository;
    }

    public List<SearchResultItem> search(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        String term = q.trim();
        List<SearchResultItem> results = new ArrayList<>();

        List<Post> posts = postRepository.searchTop10(term);
        posts.forEach(p -> {
            SearchResultItem item = new SearchResultItem();
            item.setType("POST");
            item.setTitle(p.getTitle());
            // Use slug (not ID) — slugs are stable, human-readable, and SEO-friendly.
            item.setUrl("/posts/" + p.getSlug());
            item.setSnippet(snippet(p.getContent(), term));
            results.add(item);
        });

        List<Page> pages = pageRepository.searchTop10(term);
        pages.forEach(p -> {
            SearchResultItem item = new SearchResultItem();
            item.setType("PAGE");
            item.setTitle(p.getTitle());
            // Use /pages/ (plural) to match the API and frontend route convention.
            item.setUrl("/pages/" + p.getSlug());
            item.setSnippet(snippet(p.getContent(), term));
            results.add(item);
        });

        List<SocialLink> socials = socialLinkRepository.findTop10ByLabelContainingIgnoreCase(term);
        socials.forEach(s -> {
            SearchResultItem item = new SearchResultItem();
            item.setType("SOCIAL");
            item.setTitle(s.getLabel());
            item.setUrl(s.getUrl());
            item.setSnippet(s.getUrl());
            results.add(item);
        });

        return results;
    }

    private String snippet(String content, String term) {
        if (content == null)
            return "";
        String lower = content.toLowerCase();
        int idx = lower.indexOf(term.toLowerCase());
        if (idx < 0) {
            return content.length() > 140 ? content.substring(0, 140) + "..." : content;
        }
        int start = Math.max(0, idx - 40);
        int end = Math.min(content.length(), idx + term.length() + 60);
        String snip = content.substring(start, end);
        if (start > 0)
            snip = "..." + snip;
        if (end < content.length())
            snip = snip + "...";
        return snip;
    }
}

package com.toghrak.backend.service;

import com.toghrak.backend.model.Page;
import com.toghrak.backend.model.Post;
import com.toghrak.backend.dto.SearchResultItem;
import com.toghrak.backend.repository.PageRepository;
import com.toghrak.backend.repository.PostRepository;
import com.toghrak.backend.repository.SocialLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    // Minimum characters required to trigger a search.
    // Prevents single-character queries that would match almost every post.
    private static final int MIN_QUERY_LENGTH = 2;

    // Maximum results returned per content type.
    private static final int MAX_RESULTS_PER_TYPE = 10;

    private final PostRepository postRepository;
    private final PageRepository pageRepository;
    private final SocialLinkRepository socialLinkRepository;

    public SearchService(PostRepository postRepository, PageRepository pageRepository,
            SocialLinkRepository socialLinkRepository) {
        this.postRepository = postRepository;
        this.pageRepository = pageRepository;
        this.socialLinkRepository = socialLinkRepository;
    }

    // readOnly = true: all three DB calls share one read-only transaction,
    // preventing the Hibernate session from closing between calls (safe lazy
    // access).
    @Transactional(readOnly = true)
    public List<SearchResultItem> search(String q) {
        if (q == null || q.isBlank() || q.trim().length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        String term = q.trim();
        // Split the input into individual words so that searching "spring tutorial"
        // finds posts containing "spring" AND posts containing "tutorial",
        // not only posts that contain the exact phrase "spring tutorial".
        String[] words = term.split("\\s+");

        List<SearchResultItem> results = new ArrayList<>();
        results.addAll(searchPosts(term, words));
        results.addAll(searchPages(term, words));
        results.addAll(searchSocials(term));
        return results;
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    private List<SearchResultItem> searchPosts(String fullTerm, String[] words) {
        // Use a LinkedHashMap keyed by post ID to deduplicate while preserving order.
        // Full-phrase results come first (highest relevance), then individual-word
        // hits.
        Map<Long, Post> seen = new LinkedHashMap<>();

        collectPosts(fullTerm, seen);
        if (words.length > 1) {
            for (String word : words) {
                if (word.length() >= MIN_QUERY_LENGTH && seen.size() < MAX_RESULTS_PER_TYPE) {
                    collectPosts(word, seen);
                }
            }
        }

        return seen.values().stream()
                .limit(MAX_RESULTS_PER_TYPE)
                .map(p -> {
                    SearchResultItem item = new SearchResultItem();
                    item.setType("POST");
                    item.setTitle(p.getTitle());
                    // Slug-based URL: human-readable, SEO-friendly, stable.
                    item.setUrl("/posts/" + p.getSlug());
                    item.setSnippet(snippet(p.getContent(), fullTerm));
                    return item;
                })
                .toList();
    }

    private void collectPosts(String word, Map<Long, Post> seen) {
        postRepository.searchByWord(word, MAX_RESULTS_PER_TYPE)
                .forEach(p -> seen.putIfAbsent(p.getId(), p));
    }

    // ── Pages ─────────────────────────────────────────────────────────────────

    private List<SearchResultItem> searchPages(String fullTerm, String[] words) {
        Map<Long, Page> seen = new LinkedHashMap<>();

        collectPages(fullTerm, seen);
        if (words.length > 1) {
            for (String word : words) {
                if (word.length() >= MIN_QUERY_LENGTH && seen.size() < MAX_RESULTS_PER_TYPE) {
                    collectPages(word, seen);
                }
            }
        }

        return seen.values().stream()
                .limit(MAX_RESULTS_PER_TYPE)
                .map(p -> {
                    SearchResultItem item = new SearchResultItem();
                    item.setType("PAGE");
                    item.setTitle(p.getTitle());
                    // Angular route is path: 'page' (singular) — /page/:slug.
                    // Using /pages/ (plural) hits the ** wildcard and shows 404.
                    item.setUrl("/page/" + p.getSlug());
                    item.setSnippet(snippet(p.getContent(), fullTerm));
                    return item;
                })
                .toList();
    }

    private void collectPages(String word, Map<Long, Page> seen) {
        pageRepository.searchByWord(word, MAX_RESULTS_PER_TYPE)
                .forEach(p -> seen.putIfAbsent(p.getId(), p));
    }

    // ── Social links ──────────────────────────────────────────────────────────

    private List<SearchResultItem> searchSocials(String term) {
        return socialLinkRepository.findTop10ByLabelContainingIgnoreCase(term)
                .stream()
                .map(s -> {
                    SearchResultItem item = new SearchResultItem();
                    item.setType("SOCIAL");
                    item.setTitle(s.getLabel());
                    item.setUrl(s.getUrl());
                    item.setSnippet(s.getUrl());
                    return item;
                })
                .toList();
    }

    // ── Snippet helper ────────────────────────────────────────────────────────

    private String snippet(String content, String term) {
        if (content == null)
            return "";
        String lower = content.toLowerCase();
        int idx = lower.indexOf(term.toLowerCase());
        if (idx < 0) {
            // Term not found in content (matched via title) — show opening paragraph.
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

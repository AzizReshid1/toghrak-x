package com.toghrak.backend.service.support;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Sanitizes user-generated HTML content using an allowlist strategy.
 *
 * Why jsoup over HtmlUtils.htmlEscape():
 *   htmlEscape() is output-encoding — it converts every '<' and '>' to entities,
 *   destroying all intentional HTML formatting (headings, bold, links, images).
 *   Jsoup's Safelist.relaxed() instead strips only dangerous constructs (script tags,
 *   event handlers, javascript: URLs) while preserving safe formatting tags.
 *
 * Allowed by Safelist.relaxed(): a, b, blockquote, br, caption, cite, code,
 *   col, colgroup, dd, div, dl, dt, em, figure, h1-h6, i, img, li, ol, p,
 *   pre, q, small, span, strike, strong, sub, sup, table, tbody, td, tfoot,
 *   th, thead, tr, u, ul.
 * Stripped unconditionally: script, style, iframe, form, input, and all
 *   JavaScript event attributes (onclick, onerror, etc.).
 */
@Component
public class HtmlEscapingSanitizer implements UserContentSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            // Allow target="_blank" so editors can open links in a new tab
            .addAttributes("a", "target")
            // Force safe rel on every link — prevents tab-napping and SEO spam
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");

    @Override
    public String sanitize(String input) {
        if (input == null) return "";
        return Jsoup.clean(input, SAFELIST);
    }
}

package com.toghrakx.backend.service.support;

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
        String trimmed = input.strip();
        // If the input has no block-level HTML tags it is plain text from the
        // textarea.  Convert it to HTML so [innerHTML] renders paragraphs and
        // line breaks correctly instead of collapsing all whitespace.
        if (!BLOCK_TAG.matcher(trimmed).find()) {
            StringBuilder sb = new StringBuilder();
            for (String para : trimmed.split("\\r?\\n([ \\t]*\\r?\\n)+")) {
                String p = para.strip();
                if (!p.isEmpty()) {
                    // Escape then restore single newlines as <br> tags
                    String escaped = escapeHtml(p).replaceAll("\\r?\\n", "<br>");
                    sb.append("<p>").append(escaped).append("</p>");
                }
            }
            return sb.isEmpty() ? "" : Jsoup.clean(sb.toString(), SAFELIST);
        }
        return Jsoup.clean(trimmed, SAFELIST);
    }

    /** Escapes the five XML/HTML special characters in a plain-text run. */
    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Matches opening block-level tags that signal the input already contains HTML. */
    private static final java.util.regex.Pattern BLOCK_TAG =
            java.util.regex.Pattern.compile(
                    "<(p|div|h[1-6]|ul|ol|blockquote|pre|table)[\\s/>]",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
}

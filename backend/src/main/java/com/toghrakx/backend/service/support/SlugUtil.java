package com.toghrakx.backend.service.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns titles into URL slugs, shared by posts, categories and pages.
 * <ul>
 * <li>Accents on Latin letters are removed: "Café" → "cafe".</li>
 * <li>Letters of other scripts (Uyghur, Arabic, Chinese, ...) are kept, so such titles do not
 * become empty slugs.</li>
 * <li>Everything that is not a letter or digit becomes a single "-".</li>
 * </ul>
 * Plain ASCII titles give the same slugs as before.
 */
public final class SlugUtil {

    // A combining accent that directly follows a Latin letter (é = e + ´ after NFD decomposition)
    private static final Pattern LATIN_ACCENTS = Pattern.compile("(?<=\\p{IsLatin})\\p{M}+");
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private SlugUtil() {
    }

    /** Returns the slug for {@code input}, at most {@code maxLength} characters; empty if nothing usable is left. */
    public static String slugify(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutLatinAccents = LATIN_ACCENTS.matcher(decomposed).replaceAll("");
        String slug = Normalizer.normalize(withoutLatinAccents, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        slug = NON_SLUG_CHARACTERS.matcher(slug).replaceAll("-");
        slug = EDGE_DASHES.matcher(slug).replaceAll("");
        if (slug.length() > maxLength) {
            slug = EDGE_DASHES.matcher(slug.substring(0, maxLength)).replaceAll("");
        }
        return slug;
    }
}

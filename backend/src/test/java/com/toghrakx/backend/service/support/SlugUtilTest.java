package com.toghrakx.backend.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {

    @Test
    void plainEnglishTitlesGiveTheSameSlugsAsBefore() {
        assertThat(SlugUtil.slugify("Hello World", 255)).isEqualTo("hello-world");
        assertThat(SlugUtil.slugify("  Spring Boot 3.5: What's new?  ", 255)).isEqualTo("spring-boot-3-5-what-s-new");
        assertThat(SlugUtil.slugify("a -- b", 255)).isEqualTo("a-b");
    }

    @Test
    void latinAccentsAreRemovedNotDropped() {
        assertThat(SlugUtil.slugify("Café au lait", 255)).isEqualTo("cafe-au-lait");
        assertThat(SlugUtil.slugify("Ürümchi Événement", 255)).isEqualTo("urumchi-evenement");
    }

    @Test
    void lettersOfOtherScriptsAreKept() {
        assertThat(SlugUtil.slugify("ئۇيغۇر تىلى", 255)).isEqualTo("ئۇيغۇر-تىلى");
        assertThat(SlugUtil.slugify("新闻 2026", 255)).isEqualTo("新闻-2026");
    }

    @Test
    void nothingUsableGivesAnEmptySlug() {
        assertThat(SlugUtil.slugify("!!! ???", 255)).isEmpty();
        assertThat(SlugUtil.slugify(null, 255)).isEmpty();
    }

    @Test
    void longSlugsAreCutWithoutATrailingDash() {
        assertThat(SlugUtil.slugify("abcde fghij", 6)).isEqualTo("abcde");
    }
}

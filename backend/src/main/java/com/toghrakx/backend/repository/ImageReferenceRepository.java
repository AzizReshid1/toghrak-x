package com.toghrakx.backend.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Answers "is this uploaded image still used anywhere?" before a file is deleted.
 * Spans several tables (and HTML content, which may embed images), so it is plain SQL
 * rather than a Spring Data repository of one entity.
 */
@Repository
public class ImageReferenceRepository {

    // Matching the unique stored file name anywhere (also inside HTML) errs on the safe side:
    // when in doubt the file is kept.
    private static final String IS_REFERENCED = """
            SELECT EXISTS (SELECT 1 FROM posts WHERE cover_image LIKE :name OR content LIKE :name)
                OR EXISTS (SELECT 1 FROM post_images WHERE image_url LIKE :name)
                OR EXISTS (SELECT 1 FROM pages WHERE content LIKE :name)
                OR EXISTS (SELECT 1 FROM page_images WHERE image_url LIKE :name)
                OR EXISTS (SELECT 1 FROM site_settings WHERE logo_url LIKE :name)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ImageReferenceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isReferenced(String storedFileName) {
        Boolean referenced = jdbc.queryForObject(IS_REFERENCED, Map.of("name", "%" + storedFileName + "%"),
                Boolean.class);
        return Boolean.TRUE.equals(referenced);
    }
}

package com.toghrakx.backend.repository;

import com.toghrakx.backend.model.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    @EntityGraph(attributePaths = "images")
    Optional<Page> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "images")
    List<Page> findAllByOrderByUpdatedAtDesc();

    // Native SQL: PostgreSQL lower() works on TEXT columns without the CLOB
    // restriction that Hibernate 6 imposes on @Lob fields in JPQL.
    @Query(value = """
            SELECT * FROM pages p
            WHERE lower(p.title) LIKE lower(concat('%', :term, '%'))
               OR lower(p.content_text) LIKE lower(concat('%', :term, '%'))
            ORDER BY p.updated_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Page> searchByWord(@Param("term") String term, @Param("limit") int limit);
}

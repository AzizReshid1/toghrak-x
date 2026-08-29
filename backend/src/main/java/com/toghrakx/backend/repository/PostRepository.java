package com.toghrakx.backend.repository;

import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.model.Post;
import com.toghrakx.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
        // Find all posts created by a given author (using the User entity)
        List<Post> findByAuthor(User author);

        // Find all posts under a specific category (using the Category entity)
        List<Post> findByCategory(Category category);

        // Find all posts created by an author using their ID.
        // This method leverages property path navigation (author.id)
        List<Post> findByAuthorId(Long authorId);

        @EntityGraph(attributePaths = { "category", "author" })
        @NonNull
        Page<Post> findAllBy(@NonNull Pageable pageable);

        @EntityGraph(attributePaths = { "category", "author" })
        @NonNull
        Page<Post> findByCategoryId(Long categoryId, @NonNull Pageable pageable);

        @EntityGraph(attributePaths = { "category", "author", "images" })
        @Query("select p from Post p where p.id = :id")
        Optional<Post> findWithImagesById(@Param("id") Long id);

        @EntityGraph(attributePaths = { "category", "author", "images" })
        Optional<Post> findBySlug(String slug);

        @EntityGraph(attributePaths = { "category", "author", "images" })
        List<Post> findTop6ByOrderByViewCountDescUpdatedAtDescCreatedAtDesc();

        // Returns only the numeric ID for a given slug — avoids loading the full entity
        // just to resolve the slug before incrementing view count.
        @Query("select p.id from Post p where p.slug = :slug")
        Optional<Long> findIdBySlug(@Param("slug") String slug);

        boolean existsBySlug(String slug);

        boolean existsBySlugAndIdNot(String slug, Long id);

        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("update Post p set p.viewCount = coalesce(p.viewCount, 0) + 1 where p.id = :id")
        int incrementViewCount(Long id);

        @EntityGraph(attributePaths = { "category", "author", "images" })
        List<Post> findTop20ByOrderByViewCountDescUpdatedAtDescCreatedAtDesc();

        boolean existsByAuthorId(Long authorId);

        long countByAuthorId(Long authorId);

        boolean existsByCategoryId(Long categoryId);

        long countByCategoryId(Long categoryId);

        // Native SQL: PostgreSQL lower() works on TEXT columns without the CLOB
        // restriction that Hibernate 6 imposes on @Lob fields in JPQL.
        @Query(value = """
                SELECT * FROM posts p
                WHERE lower(p.title) LIKE lower(concat('%', :term, '%'))
                   OR lower(p.content) LIKE lower(concat('%', :term, '%'))
                ORDER BY p.created_at DESC
                LIMIT :limit
                """, nativeQuery = true)
        List<Post> searchByWord(@Param("term") String term, @Param("limit") int limit);
}

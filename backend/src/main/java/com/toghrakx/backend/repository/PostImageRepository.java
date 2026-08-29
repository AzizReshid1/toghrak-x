package com.toghrakx.backend.repository;

import com.toghrakx.backend.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    // Derived query method to find all images for a given post by its ID
    List<PostImage> findByPostId(Long postId); // Useful if you need to fetch images for a post

}
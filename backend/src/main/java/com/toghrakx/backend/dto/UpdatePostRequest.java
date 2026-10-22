package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostRequest {

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    // Optional, can be null if category remains unchanged
    private Long categoryId;

    // Optional cover image URL
    @Size(max = 255, message = "Cover image URL is too long")
    private String coverImage;

    // Optional: list of image URLs to update/replace the existing images
    private List<@Size(max = 255, message = "Image URL is too long") String> imageUrls;
}

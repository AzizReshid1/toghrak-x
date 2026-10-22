package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Optional cover image URL returned by the image upload endpoint
    @Size(max = 255, message = "Cover image URL is too long")
    private String coverImage;

    // Optional list of gallery image URLs
    private List<@Size(max = 255, message = "Image URL is too long") String> imageUrls;
}

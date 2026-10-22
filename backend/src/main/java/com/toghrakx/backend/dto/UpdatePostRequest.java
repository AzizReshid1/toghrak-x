package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // PUT replaces the whole post, so this is its complete new state.
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Null or blank removes the cover image
    @Size(max = 255, message = "Cover image URL is too long")
    private String coverImage;

    // Replaces the gallery; null or empty removes all gallery images
    private List<@Size(max = 255, message = "Image URL is too long") String> imageUrls;
}

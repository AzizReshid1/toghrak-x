package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteSettingsDto {
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @Size(max = 2048, message = "Logo URL is too long")
    private String logoUrl;

    @NotBlank(message = "Slogan is required")
    @Size(max = 200, message = "Slogan must be at most 200 characters")
    private String slogan;
}

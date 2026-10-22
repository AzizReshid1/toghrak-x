package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLinkDto {
    private Long id;
    @NotBlank(message = "Label is required")
    @Size(max = 100, message = "Label must be at most 100 characters")
    private String label;
    @NotBlank(message = "Url is required")
    @Size(max = 255, message = "Url must be at most 255 characters")
    private String url;
    @Size(max = 255, message = "Icon must be at most 255 characters")
    private String icon;
}

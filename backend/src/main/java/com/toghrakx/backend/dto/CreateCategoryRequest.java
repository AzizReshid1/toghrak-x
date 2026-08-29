package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;
}

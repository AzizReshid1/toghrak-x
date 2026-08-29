package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.CategoryResponse;
import com.toghrakx.backend.dto.CreateCategoryRequest;
import com.toghrakx.backend.dto.UpdateCategoryRequest;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET all categories (public)
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        List<CategoryResponse> responses = categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // GET by slug (public)
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        Category category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(mapToResponse(category));
    }

    // GET a single category by ID (public)
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(mapToResponse(category));
    }

    // CREATE a new category (ADMIN only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category newCategory = categoryService.createCategory(request.getName(), request.getDescription());
        return new ResponseEntity<>(mapToResponse(newCategory), HttpStatus.CREATED);
    }

    // UPDATE a category (ADMIN only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        Category updatedCategory = categoryService.updateCategory(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(mapToResponse(updatedCategory));
    }

    // DELETE a category (ADMIN only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // Utility method to map Category entity to CategoryResponse DTO
    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSlug(category.getSlug());
        response.setDescription(category.getDescription());
        return response;
    }
}

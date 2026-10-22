package com.toghrakx.backend.service;

import com.toghrakx.backend.service.support.SlugUtil;
import com.toghrakx.backend.exception.ResourceNotFoundException;
import com.toghrakx.backend.model.Category;
import com.toghrakx.backend.repository.CategoryRepository;
import com.toghrakx.backend.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + id));
    }

    @Transactional(readOnly = true)
    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug=" + slug));
    }

    public Category createCategory(String name, String description) {
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        ensureSlug(category);
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, String name, String description) {
        Category existing = getCategoryById(id);
        existing.setName(name);
        existing.setDescription(description);
        ensureSlug(existing);
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Long id) {
        Category existing = getCategoryById(id);
        if (postRepository.existsByCategoryId(id)) {
            long n = postRepository.countByCategoryId(id);
            throw new IllegalStateException(
                    "Cannot delete category: there are still " + n + " post(s) assigned to it.");
        }
        categoryRepository.delete(existing);
    }

    private void ensureSlug(Category category) {
        if (category.getSlug() != null && !category.getSlug().isBlank()) {
            return;
        }
        // 140 leaves room for a "-N" suffix within the 150-character column
        String base = SlugUtil.slugify(category.getName(), 140);
        if (base.isEmpty()) {
            base = "category";
        }
        String unique = ensureUniqueSlug(base, category.getId());
        category.setSlug(unique);
    }

    private String ensureUniqueSlug(String base, Long currentId) {
        String candidate = base;
        int i = 1;
        while (true) {
            var existing = categoryRepository.findBySlug(candidate);
            if (existing.isEmpty() || (currentId != null && existing.get().getId().equals(currentId))) {
                return candidate;
            }
            candidate = base + "-" + i;
            i++;
        }
    }

}

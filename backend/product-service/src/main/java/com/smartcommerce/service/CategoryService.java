package com.smartcommerce.service;

import com.smartcommerce.dto.CategoryRequest;
import com.smartcommerce.dto.CategoryResponse;
import com.smartcommerce.entity.Category;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CacheManager cacheManager;

    @PostConstruct
    public void clearCacheOnStartup() {
        Cache cache = cacheManager.getCache("categories");
        if (cache != null) {
            cache.clear();
            log.info("Categories cache cleared on startup");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.info("Cache miss - fetching categories from DB");
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        log.info("Cache miss - fetching category from DB: id={}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category and evicting cache");
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .slug(request.getSlug())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category {} and evicting cache", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setSlug(request.getSlug());
        return toResponse(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category {} and evicting cache", id);
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .slug(category.getSlug())
                .active(category.getActive())
                .productCount((int) categoryRepository.countByCategoryId(category.getId()))
                .build();
    }
}
package com.smartcommerce.service;

import com.smartcommerce.dto.CategoryRequest;
import com.smartcommerce.dto.CategoryResponse;
import com.smartcommerce.entity.Category;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.mapper.ProductMapper;
import com.smartcommerce.repository.CategoryRepository;
import com.smartcommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> {
                    CategoryResponse response = mapper.toCategoryResponse(c);
                    response.setProductCount(
                        (int) productRepository.findByCategoryIdAndActiveTrue(
                            c.getId(), PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements()
                    );
                    return response;
                })
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return mapper.toCategoryResponse(category);
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists: " + request.getName());
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .slug(request.getSlug() != null ? request.getSlug()
                        : request.getName().toLowerCase().replace(" ", "-"))
                .build();
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        if (request.getSlug() != null) category.setSlug(request.getSlug());
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }
}

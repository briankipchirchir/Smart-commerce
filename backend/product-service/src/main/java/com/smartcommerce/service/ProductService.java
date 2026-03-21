package com.smartcommerce.service;

import com.smartcommerce.dto.ProductRequest;
import com.smartcommerce.dto.ProductResponse;
import com.smartcommerce.entity.Category;
import com.smartcommerce.entity.Product;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.mapper.ProductMapper;
import com.smartcommerce.repository.CategoryRepository;
import com.smartcommerce.repository.ProductRepository;
import com.smartcommerce.response.PagedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper mapper;

    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #sortBy")
    public PagedResult<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        log.info("Cache miss - fetching products from DB: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<ProductResponse> result = productRepository.findByActiveTrue(pageable)
                .map(mapper::toResponse);
        return PagedResult.from(result);
    }

    @Cacheable(value = "products", key = "'category-' + #categoryId + '-' + #page + '-' + #size")
    public PagedResult<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        log.info("Cache miss - fetching products by category from DB: categoryId={}", categoryId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductResponse> result = productRepository
                .findByCategoryIdAndActiveTrue(categoryId, pageable)
                .map(mapper::toResponse);
        return PagedResult.from(result);
    }

    public PagedResult<ProductResponse> searchProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductResponse> result = productRepository
                .searchProducts(query, pageable)
                .map(mapper::toResponse);
        return PagedResult.from(result);
    }

    @Cacheable(value = "featured", key = "#page + '-' + #size")
    public PagedResult<ProductResponse> getFeaturedProducts(int page, int size) {
        log.info("Cache miss - fetching featured products from DB");
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> result = productRepository
                .findByFeaturedTrueAndActiveTrue(pageable)
                .map(mapper::toResponse);
        return PagedResult.from(result);
    }

    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("Cache miss - fetching product from DB: id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapper.toResponse(product);
    }

    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "featured", allEntries = true)
    })
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product and evicting cache");
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .images(request.getImages())
                .category(category)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .brand(request.getBrand())
                .rating(0.0)
                .reviewCount(0)
                .build();

        return mapper.toResponse(productRepository.save(product));
    }

    @Caching(evict = {
        @CacheEvict(value = "product", key = "#id"),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "featured", allEntries = true)
    })
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product {} and evicting cache", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setImages(request.getImages());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());

        return mapper.toResponse(productRepository.save(product));
    }

    @Caching(evict = {
        @CacheEvict(value = "product", key = "#id"),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "featured", allEntries = true)
    })
    public void deleteProduct(Long id) {
        log.info("Deleting product {} and evicting cache", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
    }
}

package com.smartcommerce.mapper;
import java.util.ArrayList;

import com.smartcommerce.dto.CategoryResponse;
import com.smartcommerce.dto.ProductResponse;
import com.smartcommerce.entity.Category;
import com.smartcommerce.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

//    public ProductResponse toResponse(Product product) {
//        return ProductResponse.builder()
//                .id(product.getId())
//                .name(product.getName())
//                .description(product.getDescription())
//                .price(product.getPrice())
//                .originalPrice(product.getOriginalPrice())
//                .stockQuantity(product.getStockQuantity())
//                .sku(product.getSku())
//                .images(product.getImages())
//                .category(toCategoryResponse(product.getCategory()))
//                .active(product.getActive())
//                .featured(product.getFeatured())
//                .rating(product.getRating())
//                .reviewCount(product.getReviewCount())
//                .brand(product.getBrand())
//                .inStock(product.getStockQuantity() > 0)
//                .createdAt(product.getCreatedAt())
//                .updatedAt(product.getUpdatedAt())
//                .build();
//    }
public ProductResponse toResponse(Product product) {
    return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .originalPrice(product.getOriginalPrice())
            .stockQuantity(product.getStockQuantity())
            .sku(product.getSku())
            .images(product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>())
            .category(toCategoryResponse(product.getCategory()))
            .active(product.getActive())
            .featured(product.getFeatured())
            .rating(product.getRating())
            .reviewCount(product.getReviewCount())
            .brand(product.getBrand())
            .inStock(product.getStockQuantity() > 0)
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
}

    public CategoryResponse toCategoryResponse(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .slug(category.getSlug())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .build();
    }
}

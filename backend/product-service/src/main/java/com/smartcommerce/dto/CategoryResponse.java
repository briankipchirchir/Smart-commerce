package com.smartcommerce.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String slug;
    private Boolean active;
    private int productCount;
    private LocalDateTime createdAt;
}

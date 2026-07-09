package com.ecommerce.modules.catalog.application;

import com.ecommerce.modules.catalog.application.dto.*;
import com.ecommerce.modules.catalog.domain.*;
import com.ecommerce.modules.shared.domain.InvalidOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse execute(CreateCategoryRequest request, UUID tenantId) {
        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID tenantId) {
        return categoryRepository.findRootCategoriesWithChildren(tenantId).stream()
                .map(CreateCategoryUseCase::mapToResponse)
                .toList();
    }

    static CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParentId())
                .children(category.getChildren() != null ? category.getChildren().stream()
                        .map(CreateCategoryUseCase::mapToChildResponse).toList() : List.of())
                .build();
    }

    static CategoryResponse mapToChildResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParentId())
                .children(List.of())
                .build();
    }
}

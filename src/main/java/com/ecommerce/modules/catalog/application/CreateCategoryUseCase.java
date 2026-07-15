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
    public CategoryResponse execute(CreateCategoryRequest request, UUID idTienda) {
        Category category = new Category();
        category.setIdTienda(idTienda);
        category.setNombre(request.getNombre());
        category.setSlug(request.getSlug());
        category.setDescripcion(request.getDescripcion());
        category.setUrlImagen(request.getUrlImagen());

        if (request.getIdPadre() != null) {
            category.setIdPadre(request.getIdPadre());
        }

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID idTienda) {
        return categoryRepository.findRootCategoriesWithChildren(idTienda).stream()
                .map(CreateCategoryUseCase::mapToResponse)
                .toList();
    }

    static CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .nombre(category.getNombre())
                .slug(category.getSlug())
                .descripcion(category.getDescripcion())
                .urlImagen(category.getUrlImagen())
                .idPadre(category.getIdPadre())
                .children(category.getChildren() != null ? category.getChildren().stream()
                        .map(CreateCategoryUseCase::mapToChildResponse).toList() : List.of())
                .build();
    }

    static CategoryResponse mapToChildResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .nombre(category.getNombre())
                .slug(category.getSlug())
                .descripcion(category.getDescripcion())
                .urlImagen(category.getUrlImagen())
                .idPadre(category.getIdPadre())
                .children(List.of())
                .build();
    }
}

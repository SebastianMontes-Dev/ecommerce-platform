package com.ecommerce.modules.catalog.application;

import com.ecommerce.modules.catalog.application.dto.*;
import com.ecommerce.modules.catalog.domain.*;
import com.ecommerce.modules.shared.domain.BusinessRuleViolationException;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.domain.Money;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
import com.ecommerce.modules.shared.domain.DomainEventPublisher;
import com.ecommerce.modules.catalog.domain.events.ProductCreatedEvent;
import com.ecommerce.modules.tenant.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public ProductResponse execute(CreateProductRequest request, UUID idTienda) {
        if (productRepository.countByIdTienda(idTienda) >= 999999) {
            throw new BusinessRuleViolationException("Se ha alcanzado el número máximo de productos para su plan");
        }

        Product product = new Product();
        product.setIdTienda(idTienda);
        product.setNombre(request.getNombre());
        product.setSlug(request.getSlug());
        product.setDescripcion(request.getDescripcion());
        product.setPrecio(Money.of(request.getPrecio(), request.getMoneda()));

        if (request.getPrecioComparacion() != null) {
            product.setPrecioComparacion(Money.of(request.getPrecioComparacion(), request.getMoneda()));
        }
        if (request.getPrecioCosto() != null) {
            product.setPrecioCosto(Money.of(request.getPrecioCosto(), request.getMoneda()));
        }

        product.setSku(request.getSku());
        product.setCodigoBarras(request.getCodigoBarras());
        product.setInventario(request.getInventario());
        product.setRastreoInventarioHabilitado(request.isRastreoInventarioHabilitado());
        product.setEstado(ProductStatus.DRAFT);

        if (request.getIdCategoria() != null) {
            product.setIdCategoria(request.getIdCategoria());
        }

        product = productRepository.save(product);

        product.markAsCreated();
        eventPublisher.publish(product.getDomainEvents());
        product.clearDomainEvents();

        return mapToResponse(product);
    }

    static ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .idTienda(product.getIdTienda())
                .nombre(product.getNombre())
                .slug(product.getSlug())
                .descripcion(product.getDescripcion())
                .precio(product.getPrecio() != null ? product.getPrecio().getMonto() : null)
                .currency(product.getPrecio() != null ? product.getPrecio().getMoneda() : "USD")
                .precioComparacion(product.getPrecioComparacion() != null ? product.getPrecioComparacion().getMonto() : null)
                .precioCosto(product.getPrecioCosto() != null ? product.getPrecioCosto().getMonto() : null)
                .sku(product.getSku())
                .codigoBarras(product.getCodigoBarras())
                .inventario(product.getInventario())
                .rastreoInventarioHabilitado(product.isRastreoInventarioHabilitado())
                .estado(product.getEstado().name())
                .idCategoria(product.getIdCategoria())
                .nombreCategoria(product.getCategory() != null ? product.getCategory().getNombre() : null)
                .variants(product.getVariants() != null ? product.getVariants().stream().map(v ->
                        ProductVariantResponse.builder()
                                .id(v.getId())
                                .nombre(v.getNombre())
                                .sku(v.getSku())
                                .precio(v.getMonto())
                                .currency(v.getMoneda())
                                .inventario(v.getInventario())
                                .attributes(v.getAttributes())
                                .build()
                ).toList() : List.of())
                .images(product.getImages() != null ? product.getImages().stream().map(i ->
                        ProductImageResponse.builder()
                                .id(i.getId())
                                .url(i.getUrl())
                                .altText(i.getAltText())
                                .width(i.getWidth())
                                .height(i.getHeight())
                                .sortOrder(i.getSortOrder())
                                .build()
                ).toList() : List.of())
                .creadoEn(product.getCreadoEn())
                .actualizadoEn(product.getActualizadoEn())
                .build();
    }
}

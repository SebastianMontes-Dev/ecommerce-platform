package com.ecommerce.modules.search.application;

import com.ecommerce.modules.catalog.domain.events.ProductCreatedEvent;
import com.ecommerce.modules.search.domain.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchEventConsumer {

    private final SearchService searchService;

    @Async
    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Received ProductCreatedEvent for product: {}", event.getIdProducto());
        
        ProductDocument doc = new ProductDocument();
        doc.setId(event.getIdProducto().toString());
        doc.setIdTienda(event.getIdTienda());
        doc.setNombre(event.getNombre());
        doc.setSlug(event.getSlug());
        doc.setDescripcion(event.getDescripcion());
        doc.setPrecio(BigDecimal.ZERO); // Normally comes from event or API
        
        searchService.indexProduct(doc);
    }
}

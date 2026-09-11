package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;

import java.util.List;

/**
 * Página de resultados de {@link ServicioBusqueda#busqueda}: el contenido de la página actual
 * más el total real de hits que reporta Elasticsearch (no el tamaño de {@code content}), para que
 * el controller pueda calcular {@code totalPages} correctamente.
 */
public record ResultadoBusqueda(List<DocumentoProducto> content, long totalElements) {
}

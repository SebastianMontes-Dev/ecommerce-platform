package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve la tienda que un usuario posee (Inquilino.idPropietario) de forma
 * cacheada. Es la única fuente confiable de idTienda para acciones de
 * gestión de tienda — nunca debe sustituirse por un valor provisto por el cliente.
 */
@Service
@RequiredArgsConstructor
public class ServicioResolutorInquilino {

    private final RepositorioInquilino repositorioInquilino;

    @Cacheable(cacheNames = "tenant-por-propietario", key = "#idUsuario", unless = "#result == null || !#result.isPresent()")
    public Optional<UUID> resolverTiendaPropia(UUID idUsuario) {
        return repositorioInquilino.findByIdPropietario(idUsuario).map(Inquilino::getId);
    }
}

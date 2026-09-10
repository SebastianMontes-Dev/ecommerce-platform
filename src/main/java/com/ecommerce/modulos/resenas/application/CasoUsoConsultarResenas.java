package com.ecommerce.modulos.resenas.application;

import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.resenas.application.dto.RespuestaResena;
import com.ecommerce.modulos.resenas.domain.RepositorioResena;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoConsultarResenas {

    private final RepositorioResena repositorioResena;

    @Transactional(readOnly = true)
    public RespuestaPaginada<RespuestaResena> listarActivasDeProducto(UUID idProducto, Pageable pageable) {
        return RespuestaPaginada.from(
                repositorioResena.findAllByIdProductoAndActivoTrue(idProducto, pageable)
                        .map(RespuestaResena::de));
    }
}

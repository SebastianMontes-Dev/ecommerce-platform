package com.ecommerce.modulos.resenas.application;

import com.ecommerce.modulos.compartido.domain.Calificacion;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.application.ServicioConsultaOrden;
import com.ecommerce.modulos.resenas.application.dto.RespuestaResena;
import com.ecommerce.modulos.resenas.application.dto.SolicitudCrearResena;
import com.ecommerce.modulos.resenas.domain.RepositorioResena;
import com.ecommerce.modulos.resenas.domain.Resena;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoCrearResena {

    private final RepositorioResena repositorioResena;
    private final ServicioConsultaOrden servicioConsultaOrden;

    @Transactional
    public RespuestaResena ejecutar(UUID idProducto, UUID idCliente, SolicitudCrearResena solicitud) {
        if (idCliente == null) {
            throw new ExcepcionOperacionInvalida("Debe estar autenticado para crear una reseña.");
        }

        // "Compra verificada": la orden es del cliente, está entregada y contiene el producto.
        servicioConsultaOrden.verificarElegibilidadResena(solicitud.getIdOrden(), idCliente, idProducto);

        Resena resena = new Resena();
        resena.setIdTienda(ContextoInquilino.getIdTienda());
        resena.setIdProducto(idProducto);
        resena.setIdCliente(idCliente);
        resena.setIdOrden(solicitud.getIdOrden());
        resena.setCalificacion(Calificacion.of(solicitud.getCalificacion()));
        resena.setTitulo(solicitud.getTitulo());
        resena.setComentario(solicitud.getComentario());

        return RespuestaResena.de(repositorioResena.save(resena));
    }
}

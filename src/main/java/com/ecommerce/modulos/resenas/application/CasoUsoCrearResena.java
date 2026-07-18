package com.ecommerce.modulos.resenas.application;

import com.ecommerce.modulos.compartido.domain.Calificacion;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
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
    private final RepositorioOrden repositorioOrden;

    @Transactional
    public Resena ejecutar(UUID idProducto, UUID idCliente, SolicitudCrearResena solicitud) {
        
        if (idCliente == null) {
            throw new ExcepcionOperacionInvalida("Debe estar autenticado para crear una reseña.");
        }

        Orden orden = repositorioOrden.findById(solicitud.getIdOrden())
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", solicitud.getIdOrden()));
                
        if (!orden.getIdCliente().equals(idCliente)) {
            throw new ExcepcionOperacionInvalida("La orden no pertenece al usuario autenticado.");
        }
        
        if (orden.getEstado() != EstadoOrden.DELIVERED) {
            throw new ExcepcionOperacionInvalida("Solo se pueden reseñar productos de órdenes entregadas.");
        }
        
        boolean contieneProducto = orden.getArticulos().stream()
                .anyMatch(articulo -> articulo.getIdProducto().equals(idProducto));
                
        if (!contieneProducto) {
            throw new ExcepcionOperacionInvalida("La orden no contiene el producto especificado.");
        }

        Resena resena = new Resena();
        resena.setIdTienda(ContextoInquilino.getIdTienda());
        resena.setIdProducto(idProducto);
        resena.setIdCliente(idCliente);
        resena.setIdOrden(solicitud.getIdOrden());
        resena.setCalificacion(Calificacion.of(solicitud.getCalificacion()));
        resena.setTitulo(solicitud.getTitulo());
        resena.setComentario(solicitud.getComentario());
        
        return repositorioResena.save(resena);
    }
}

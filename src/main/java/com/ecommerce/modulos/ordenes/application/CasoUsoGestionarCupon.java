package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionRecursoDuplicado;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCrearCupon;
import com.ecommerce.modulos.ordenes.domain.Cupon;
import com.ecommerce.modulos.ordenes.domain.RepositorioCupon;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoGestionarCupon {

    private final RepositorioCupon repositorioCupon;

    @Transactional
    public Cupon crearCupon(UUID idTienda, SolicitudCrearCupon request) {
        repositorioCupon.findByIdTiendaAndCodigo(idTienda, request.getCodigo().toUpperCase())
                .ifPresent(c -> {
                    throw new ExcepcionRecursoDuplicado("Cupón", "código", request.getCodigo());
                });

        Cupon cupon = new Cupon();
        cupon.setIdTienda(idTienda);
        cupon.setCodigo(request.getCodigo().toUpperCase());
        cupon.setTipo(request.getTipo());
        cupon.setValor(request.getValor());
        cupon.setFechaExpiracion(request.getFechaExpiracion());
        cupon.setLimiteUsos(request.getLimiteUsos());
        
        return repositorioCupon.save(cupon);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<Cupon> listarCupones(UUID idTienda, Pageable pageable) {
        Page<Cupon> page = repositorioCupon.findAllByIdTienda(idTienda, pageable);
        return RespuestaPaginada.from(page);
    }

    @Transactional
    public void alternarEstadoCupon(UUID idTienda, UUID idCupon) {
        Cupon cupon = repositorioCupon.findById(idCupon)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Cupón", idCupon));
                
        if (!cupon.getIdTienda().equals(idTienda)) {
            throw new ExcepcionEntidadNoEncontrada("Cupón", idCupon);
        }
        
        cupon.setActivo(!cupon.isActivo());
        repositorioCupon.save(cupon);
    }

    @Transactional(readOnly = true)
    public Cupon validarYObtenerCupon(UUID idTienda, String codigo) {
        Cupon cupon = repositorioCupon.findByIdTiendaAndCodigo(idTienda, codigo.toUpperCase())
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Cupón", codigo));
                
        if (!cupon.esValido()) {
            throw new IllegalArgumentException("El cupón ingresado ha expirado, alcanzó su límite de usos o está inactivo.");
        }
        
        return cupon;
    }
}

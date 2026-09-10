package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionRecursoDuplicado;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCrearCupon;
import com.ecommerce.modulos.ordenes.domain.Cupon;
import com.ecommerce.modulos.ordenes.domain.RepositorioCupon;
import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Valida el cupón y calcula el descuento para un {@code subtotal} dado (porcentaje o
     * monto fijo). Así el módulo carrito no tiene que conocer {@link Cupon} ni
     * {@link TipoDescuento} para aplicar un cupón.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularDescuento(UUID idTienda, String codigo, BigDecimal subtotal) {
        Cupon cupon = validarYObtenerCupon(idTienda, codigo);
        BigDecimal descuento = cupon.getTipo() == TipoDescuento.PORCENTAJE
                ? subtotal.multiply(cupon.getValor()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : cupon.getValor();
        return descuento.min(subtotal).max(BigDecimal.ZERO);
    }
}

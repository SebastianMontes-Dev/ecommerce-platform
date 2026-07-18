package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.compartido.domain.Direccion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitudCheckout {
    
    @NotNull
    private Direccion direccionEnvio;
    
    @NotNull
    private Direccion direccionFacturacion;
    
    private String notas;
}

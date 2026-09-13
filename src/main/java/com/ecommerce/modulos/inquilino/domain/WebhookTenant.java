package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "webhook_tenants")
@Getter
@Setter
@NoArgsConstructor
public class WebhookTenant extends EntidadInquilino {

    private String urlDestino;
    private String evento;
    private String secret;

    public WebhookTenant(UUID idTienda, String urlDestino, String evento, String secret) {
        this.setIdTienda(idTienda);
        this.urlDestino = urlDestino;
        this.evento = evento;
        this.secret = secret;
    }
}

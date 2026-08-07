package com.ecommerce.modulos.inquilino.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "webhook_tenants")
public class WebhookTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID idTienda;
    private String urlDestino;
    private String evento;
    private String secret;

    public WebhookTenant() {
    }

    public WebhookTenant(UUID idTienda, String urlDestino, String evento, String secret) {
        this.idTienda = idTienda;
        this.urlDestino = urlDestino;
        this.evento = evento;
        this.secret = secret;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdTienda() {
        return idTienda;
    }

    public String getUrlDestino() {
        return urlDestino;
    }

    public String getEvento() {
        return evento;
    }

    public String getSecret() {
        return secret;
    }
}

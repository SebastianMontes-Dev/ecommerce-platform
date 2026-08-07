package com.ecommerce.modulos.inquilino.domain;

import java.util.List;
import java.util.UUID;

public interface RepositorioWebhookTenant {
    List<WebhookTenant> buscarPorIdTiendaYEvento(UUID idTienda, String evento);
}

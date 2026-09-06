package com.ecommerce.modulos.inquilino.application;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Defensa contra SSRF para URLs de destino configuradas por el vendedor (no por la
 * plataforma). Rechaza esquemas distintos de http/https y cualquier host que resuelva a
 * una direccion no publica (loopback, red privada, link-local -incluyendo el metadata
 * endpoint de la nube en 169.254.169.254-, multicast o wildcard), para que un vendedor
 * no pueda usar un envio saliente como proxy hacia infraestructura interna de la
 * plataforma.
 */
@Component
public class ValidadorUrlWebhook {

    private static final Set<String> ESQUEMAS_PERMITIDOS = Set.of("http", "https");

    public boolean esSegura(String urlDestino) {
        try {
            URI uri = URI.create(urlDestino);
            String esquema = uri.getScheme();
            String host = uri.getHost();
            if (esquema == null || host == null || !ESQUEMAS_PERMITIDOS.contains(esquema.toLowerCase())) {
                return false;
            }

            for (InetAddress direccion : InetAddress.getAllByName(host)) {
                if (direccion.isLoopbackAddress()
                        || direccion.isSiteLocalAddress()
                        || direccion.isLinkLocalAddress()
                        || direccion.isMulticastAddress()
                        || direccion.isAnyLocalAddress()) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException | UnknownHostException e) {
            return false;
        }
    }
}

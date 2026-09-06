package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.RepositorioWebhookTenant;
import com.ecommerce.modulos.inquilino.domain.WebhookTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.Formatter;

@Service
public class ServicioEmisorWebhook {

    private static final Logger log = LoggerFactory.getLogger(ServicioEmisorWebhook.class);
    private static final int MAX_INTENTOS = 3;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 500;

    private final RepositorioWebhookTenant repositorioWebhookTenant;
    private final RestTemplate restTemplate;
    private final ValidadorUrlWebhook validadorUrlWebhook;

    public ServicioEmisorWebhook(RepositorioWebhookTenant repositorioWebhookTenant, RestTemplate restTemplate,
                                  ValidadorUrlWebhook validadorUrlWebhook) {
        this.repositorioWebhookTenant = repositorioWebhookTenant;
        this.restTemplate = restTemplate;
        this.validadorUrlWebhook = validadorUrlWebhook;
    }

    public void emitirEvento(UUID idTienda, String evento, String payloadJson) {
        List<WebhookTenant> webhooks = repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, evento);

        for (WebhookTenant webhook : webhooks) {
            enviarWebhook(webhook, payloadJson);
        }
    }

    private void enviarWebhook(WebhookTenant webhook, String payloadJson) {
        if (!validadorUrlWebhook.esSegura(webhook.getUrlDestino())) {
            log.error("Webhook a {} rechazado: la URL de destino no es publica (SSRF), se descarta sin enviar",
                    webhook.getUrlDestino());
            return;
        }

        String signature;
        try {
            signature = calcularHMAC(payloadJson, webhook.getSecret());
        } catch (Exception e) {
            log.error("No se pudo calcular la firma HMAC para el webhook a {}: {}", webhook.getUrlDestino(), e.getMessage(), e);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-NexaSaaS-Signature", signature);
        HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                restTemplate.postForObject(webhook.getUrlDestino(), request, String.class);
                return;
            } catch (Exception e) {
                if (intento == MAX_INTENTOS) {
                    log.error("Webhook a {} fallo tras {} intentos, se descarta: {}",
                            webhook.getUrlDestino(), MAX_INTENTOS, e.getMessage(), e);
                    return;
                }
                log.warn("Intento {}/{} fallido enviando webhook a {}: {} - reintentando",
                        intento, MAX_INTENTOS, webhook.getUrlDestino(), e.getMessage());
                esperarAntesDeReintentar();
            }
        }
    }

    private void esperarAntesDeReintentar() {
        try {
            Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String calcularHMAC(String data, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String hex = formatter.toString();
        formatter.close();
        return hex;
    }
}

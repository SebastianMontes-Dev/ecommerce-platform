package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.RepositorioWebhookTenant;
import com.ecommerce.modulos.inquilino.domain.WebhookTenant;
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

    private final RepositorioWebhookTenant repositorioWebhookTenant;
    private final RestTemplate restTemplate;

    public ServicioEmisorWebhook(RepositorioWebhookTenant repositorioWebhookTenant, RestTemplate restTemplate) {
        this.repositorioWebhookTenant = repositorioWebhookTenant;
        this.restTemplate = restTemplate;
    }

    public void emitirEvento(UUID idTienda, String evento, String payloadJson) {
        List<WebhookTenant> webhooks = repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, evento);
        
        for (WebhookTenant webhook : webhooks) {
            enviarWebhook(webhook, payloadJson);
        }
    }

    private void enviarWebhook(WebhookTenant webhook, String payloadJson) {
        try {
            String signature = calcularHMAC(payloadJson, webhook.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-NexaSaaS-Signature", signature);

            HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
            restTemplate.postForObject(webhook.getUrlDestino(), request, String.class);
        } catch (Exception e) {
            // Logear error de envío
            System.err.println("Error al enviar webhook a " + webhook.getUrlDestino() + ": " + e.getMessage());
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

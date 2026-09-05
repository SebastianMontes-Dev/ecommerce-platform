package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.RepositorioWebhookTenant;
import com.ecommerce.modulos.inquilino.domain.WebhookTenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioEmisorWebhookTest {

    @Mock
    private RepositorioWebhookTenant repositorioWebhookTenant;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ServicioEmisorWebhook servicioEmisorWebhook;

    private final UUID idTienda = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private HttpEntity<String> capturarPeticionEnviada() {
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(String.class));
        return captor.getValue();
    }

    @Test
    void debeEnviarWebhookConFirmaHmacCuandoHayUnaSuscripcionRegistradaParaElEvento() {
        // Arrange
        WebhookTenant webhook = new WebhookTenant(idTienda, "https://cliente.com/webhook", "orden.creada", "secreto-123");
        when(repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, "orden.creada"))
                .thenReturn(List.of(webhook));

        // Act
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":1}");

        // Assert
        HttpEntity<String> peticionEnviada = capturarPeticionEnviada();
        assertEquals("{\"id\":1}", peticionEnviada.getBody());
        String firma = peticionEnviada.getHeaders().getFirst("X-NexaSaaS-Signature");
        assertNotNull(firma);
        assertTrue(firma.matches("^[0-9a-f]{64}$"), "La firma debe ser un hex de 64 caracteres (SHA-256)");
    }

    @Test
    void debeGenerarFirmasDistintasParaPayloadsDistintosConElMismoSecret() {
        // Arrange
        WebhookTenant webhook = new WebhookTenant(idTienda, "https://cliente.com/webhook", "orden.creada", "secreto-123");
        when(repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, "orden.creada"))
                .thenReturn(List.of(webhook));

        // Act
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":1}");
        String firma1 = capturarPeticionEnviada().getHeaders().getFirst("X-NexaSaaS-Signature");

        org.mockito.Mockito.clearInvocations(restTemplate);
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":2}");
        String firma2 = capturarPeticionEnviada().getHeaders().getFirst("X-NexaSaaS-Signature");

        // Assert
        assertNotEquals(firma1, firma2);
    }

    @Test
    void debeGenerarLaMismaFirmaParaElMismoPayloadYSecretSiempre() {
        // Arrange
        WebhookTenant webhook = new WebhookTenant(idTienda, "https://cliente.com/webhook", "orden.creada", "secreto-123");
        when(repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, "orden.creada"))
                .thenReturn(List.of(webhook));

        // Act
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":1}");
        String firma1 = capturarPeticionEnviada().getHeaders().getFirst("X-NexaSaaS-Signature");

        org.mockito.Mockito.clearInvocations(restTemplate);
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":1}");
        String firma2 = capturarPeticionEnviada().getHeaders().getFirst("X-NexaSaaS-Signature");

        // Assert
        assertEquals(firma1, firma2);
    }

    @Test
    void debeEnviarATodosLosWebhooksRegistradosCuandoHayMasDeUnoParaElMismoEvento() {
        // Arrange
        WebhookTenant webhook1 = new WebhookTenant(idTienda, "https://cliente1.com/webhook", "orden.creada", "secreto-1");
        WebhookTenant webhook2 = new WebhookTenant(idTienda, "https://cliente2.com/webhook", "orden.creada", "secreto-2");
        when(repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, "orden.creada"))
                .thenReturn(List.of(webhook1, webhook2));

        // Act
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":1}");

        // Assert
        verify(restTemplate).postForObject(eq("https://cliente1.com/webhook"), any(HttpEntity.class), eq(String.class));
        verify(restTemplate).postForObject(eq("https://cliente2.com/webhook"), any(HttpEntity.class), eq(String.class));
        verify(restTemplate, times(2)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void noDebeLlamarAlClienteHttpCuandoNoHayWebhooksRegistradosParaElEvento() {
        // Arrange
        when(repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, "orden.cancelada"))
                .thenReturn(List.of());

        // Act
        servicioEmisorWebhook.emitirEvento(idTienda, "orden.cancelada", "{\"id\":1}");

        // Assert
        verifyNoInteractions(restTemplate);
    }

    @Test
    void debeContinuarSinLanzarExcepcionCuandoElEnvioHttpFalla() {
        // Arrange
        WebhookTenant webhookFalla = new WebhookTenant(idTienda, "https://caido.com/webhook", "orden.creada", "secreto-1");
        WebhookTenant webhookOk = new WebhookTenant(idTienda, "https://ok.com/webhook", "orden.creada", "secreto-2");
        when(repositorioWebhookTenant.buscarPorIdTiendaYEvento(idTienda, "orden.creada"))
                .thenReturn(List.of(webhookFalla, webhookOk));
        when(restTemplate.postForObject(eq("https://caido.com/webhook"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        // Act & Assert: no debe propagar la excepción del primer webhook y debe seguir con el segundo
        assertDoesNotThrow(() -> servicioEmisorWebhook.emitirEvento(idTienda, "orden.creada", "{\"id\":1}"));
        verify(restTemplate).postForObject(eq("https://ok.com/webhook"), any(HttpEntity.class), eq(String.class));
    }
}

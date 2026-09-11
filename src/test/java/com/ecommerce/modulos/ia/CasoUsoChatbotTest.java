package com.ecommerce.modulos.ia;

import com.ecommerce.modulos.compartido.domain.ExcepcionServicioExterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasoUsoChatbotTest {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Mock
    private RestTemplate restTemplate;

    private CasoUsoChatbot casoUsoChatbot;

    @BeforeEach
    void setUp() {
        casoUsoChatbot = new CasoUsoChatbot(restTemplate);
    }

    @Test
    void debeDevolverContenidoDeRespuestaSiOpenAiRespondeConChoices() {
        UUID idTienda = UUID.randomUUID();
        String prompt = "¿Cuál es el horario de la tienda?";

        OpenAiResponseDto.Message mensaje = new OpenAiResponseDto.Message();
        mensaje.setRole("assistant");
        mensaje.setContent("El horario es de 9 a 18hs.");
        OpenAiResponseDto.Choice choice = new OpenAiResponseDto.Choice();
        choice.setMessage(mensaje);
        OpenAiResponseDto responseDto = new OpenAiResponseDto();
        responseDto.setChoices(List.of(choice));

        when(restTemplate.postForEntity(eq(OPENAI_API_URL), any(HttpEntity.class), eq(OpenAiResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        String resultado = casoUsoChatbot.procesarChat(prompt, idTienda);

        assertEquals("El horario es de 9 a 18hs.", resultado);
    }

    @Test
    void debeEnviarPromptConContextoDeTiendaAOpenAi() {
        UUID idTienda = UUID.randomUUID();
        String prompt = "hola";

        when(restTemplate.postForEntity(eq(OPENAI_API_URL), any(HttpEntity.class), eq(OpenAiResponseDto.class)))
                .thenReturn(ResponseEntity.ok(new OpenAiResponseDto()));

        casoUsoChatbot.procesarChat(prompt, idTienda);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<OpenAiRequestDto>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(eq(OPENAI_API_URL), captor.capture(), eq(OpenAiResponseDto.class));

        OpenAiRequestDto requestEnviado = captor.getValue().getBody();
        assertEquals(1, requestEnviado.getMessages().size());
        assertEquals("Contexto tienda " + idTienda + ": hola", requestEnviado.getMessages().get(0).getContent());
    }

    @Test
    void debeDevolverMensajePorDefectoSiBodyEsNulo() {
        when(restTemplate.postForEntity(eq(OPENAI_API_URL), any(HttpEntity.class), eq(OpenAiResponseDto.class)))
                .thenReturn(ResponseEntity.ok(null));

        String resultado = casoUsoChatbot.procesarChat("hola", UUID.randomUUID());

        assertEquals("No se pudo obtener respuesta del chatbot.", resultado);
    }

    @Test
    void debeDevolverMensajePorDefectoSiChoicesEsNulo() {
        OpenAiResponseDto responseDto = new OpenAiResponseDto();
        responseDto.setChoices(null);

        when(restTemplate.postForEntity(eq(OPENAI_API_URL), any(HttpEntity.class), eq(OpenAiResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        String resultado = casoUsoChatbot.procesarChat("hola", UUID.randomUUID());

        assertEquals("No se pudo obtener respuesta del chatbot.", resultado);
    }

    @Test
    void debeDevolverMensajePorDefectoSiChoicesEstaVacio() {
        OpenAiResponseDto responseDto = new OpenAiResponseDto();
        responseDto.setChoices(List.of());

        when(restTemplate.postForEntity(eq(OPENAI_API_URL), any(HttpEntity.class), eq(OpenAiResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        String resultado = casoUsoChatbot.procesarChat("hola", UUID.randomUUID());

        assertEquals("No se pudo obtener respuesta del chatbot.", resultado);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Si OpenAI falla, lanza ExcepcionServicioExterno en vez de devolver el detalle como respuesta 200")
    void debeLanzarExcepcionServicioExternoSiRestTemplateFalla() {
        when(restTemplate.postForEntity(eq(OPENAI_API_URL), any(HttpEntity.class), eq(OpenAiResponseDto.class)))
                .thenThrow(new RestClientException("Timeout al conectar con OpenAI"));

        assertThrows(ExcepcionServicioExterno.class,
                () -> casoUsoChatbot.procesarChat("hola", UUID.randomUUID()));
    }
}

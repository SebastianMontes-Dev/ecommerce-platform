package com.ecommerce.modulos.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

@Service
public class CasoUsoChatbot {

    @Value("${openai.api.key:sk-mock-key-for-testing}")
    private String apiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;

    public CasoUsoChatbot() {
        this.restTemplate = new RestTemplate();
    }

    public String procesarChat(String prompt, String tenantId) {
        String promptConContexto = "Contexto tienda " + tenantId + ": " + prompt;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        OpenAiRequestDto requestDto = new OpenAiRequestDto(promptConContexto);
        HttpEntity<OpenAiRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<OpenAiResponseDto> response = restTemplate.postForEntity(OPENAI_API_URL, entity, OpenAiResponseDto.class);
            
            if (response.getBody() != null && response.getBody().getChoices() != null && !response.getBody().getChoices().isEmpty()) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            return "Error al comunicarse con OpenAI: " + e.getMessage();
        }

        return "No se pudo obtener respuesta del chatbot.";
    }
}

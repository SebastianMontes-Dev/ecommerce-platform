package com.ecommerce.modulos.ia;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ControladorIA {

    private final CasoUsoChatbot casoUsoChatbot;

    public ControladorIA(CasoUsoChatbot casoUsoChatbot) {
        this.casoUsoChatbot = casoUsoChatbot;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chatear(@RequestBody ChatRequestDto request) {
        String respuesta = casoUsoChatbot.procesarChat(request.getPrompt(), request.getTenantId());
        return ResponseEntity.ok(new ChatResponseDto(respuesta));
    }
}

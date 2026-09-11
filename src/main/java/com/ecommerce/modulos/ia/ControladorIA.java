package com.ecommerce.modulos.ia;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/chatbot")
@Tag(name = "Chatbot", description = "Asistente de IA con contexto de la tienda del usuario autenticado")
public class ControladorIA {

    private final CasoUsoChatbot casoUsoChatbot;

    public ControladorIA(CasoUsoChatbot casoUsoChatbot) {
        this.casoUsoChatbot = casoUsoChatbot;
    }

    @PostMapping("/chat")
    @Operation(summary = "Enviar un mensaje al chatbot de la tienda actual")
    public ResponseEntity<ChatResponseDto> chatear(@Valid @RequestBody ChatRequestDto request) {
        String respuesta = casoUsoChatbot.procesarChat(request.getPrompt(), ContextoInquilino.getIdTienda());
        return ResponseEntity.ok(new ChatResponseDto(respuesta));
    }
}

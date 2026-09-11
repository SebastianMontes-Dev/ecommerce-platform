package com.ecommerce.modulos.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ya no lleva {@code tenantId}: antes lo mandaba el cliente en el body y
 * {@link CasoUsoChatbot} lo usaba tal cual, permitiendo que cualquier usuario autenticado
 * consultara el chatbot "en nombre" de otra tienda. El tenant sale de
 * {@link com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino}, igual que en
 * el resto de los controllers.
 */
public class ChatRequestDto {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String prompt;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}

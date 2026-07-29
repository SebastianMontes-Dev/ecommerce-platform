package com.ecommerce.modulos.ia;

public class ChatResponseDto {
    private String respuesta;

    public ChatResponseDto() {}
    public ChatResponseDto(String respuesta) { this.respuesta = respuesta; }
    
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
}

package com.ecommerce.modulos.ia;

public class ChatRequestDto {
    private String prompt;
    private String tenantId;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}

package com.ecommerce.modulos.ia;

import java.util.List;
import java.util.ArrayList;

public class OpenAiRequestDto {
    private String model = "gpt-3.5-turbo";
    private List<Message> messages = new ArrayList<>();

    public OpenAiRequestDto() {}

    public OpenAiRequestDto(String prompt) {
        this.messages.add(new Message("user", prompt));
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public static class Message {
        private String role;
        private String content;

        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

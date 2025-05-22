package com.example.chatgptapp.dto;

import java.util.List;

public class OpenAIChatRequest {
    private String model;
    private List<MessageDto> messages;

    public OpenAIChatRequest() {
    }

    public OpenAIChatRequest(String model, List<MessageDto> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<MessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
    }
}

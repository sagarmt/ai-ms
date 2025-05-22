package com.example.chatgptapp.dto;

public class OpenAIEmbeddingRequest {
    private String input;
    private String model;

    public OpenAIEmbeddingRequest() {
    }

    public OpenAIEmbeddingRequest(String input, String model) {
        this.input = input;
        this.model = model;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

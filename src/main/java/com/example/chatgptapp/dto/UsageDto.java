package com.example.chatgptapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UsageDto {
    @JsonProperty("prompt_tokens")
    private int promptTokens;

    @JsonProperty("total_tokens")
    private int totalTokens;

    public UsageDto() {
    }

    public UsageDto(int promptTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
}

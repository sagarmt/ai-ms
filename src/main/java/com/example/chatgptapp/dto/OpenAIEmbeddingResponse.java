package com.example.chatgptapp.dto;

import java.util.List;

public class OpenAIEmbeddingResponse {
    private String object;
    private List<EmbeddingDataDto> data;
    private String model;
    private UsageDto usage;

    public OpenAIEmbeddingResponse() {
    }

    public OpenAIEmbeddingResponse(String object, List<EmbeddingDataDto> data, String model, UsageDto usage) {
        this.object = object;
        this.data = data;
        this.model = model;
        this.usage = usage;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<EmbeddingDataDto> getData() {
        return data;
    }

    public void setData(List<EmbeddingDataDto> data) {
        this.data = data;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public UsageDto getUsage() {
        return usage;
    }

    public void setUsage(UsageDto usage) {
        this.usage = usage;
    }
}

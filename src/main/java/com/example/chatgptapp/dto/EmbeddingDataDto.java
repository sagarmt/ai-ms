package com.example.chatgptapp.dto;

import java.util.List;

public class EmbeddingDataDto {
    private String object;
    private List<Double> embedding;
    private int index;

    public EmbeddingDataDto() {
    }

    public EmbeddingDataDto(String object, List<Double> embedding, int index) {
        this.object = object;
        this.embedding = embedding;
        this.index = index;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}

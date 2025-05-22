package com.example.chatgptapp.dto;

import java.util.List;

public class VectorData {
    private String id;
    private String content;
    private List<Double> embedding;

    public VectorData() {
    }

    public VectorData(String id, String content, List<Double> embedding) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "VectorData{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", embedding_size=" + (embedding != null ? embedding.size() : 0) +
                '}';
    }
}

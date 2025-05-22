package com.example.chatgptapp.service;

import com.example.chatgptapp.dto.VectorData;
import java.util.List;

public interface VectorStoreService {

    /**
     * Stores a vector data object.
     *
     * @param vectorData The vector data to store.
     */
    void store(VectorData vectorData);

    /**
     * Finds the topK most similar vectors to the query embedding.
     *
     * @param queryEmbedding The embedding of the query.
     * @param topK The number of similar items to return.
     * @return A list of the topK most similar VectorData objects.
     */
    List<VectorData> findSimilar(List<Double> queryEmbedding, int topK);

    /**
     * Clears all data from the vector store.
     * Useful for in-memory implementations or testing.
     */
    void clear();
}

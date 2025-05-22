package com.example.chatgptapp.service.vectorstore;

import com.example.chatgptapp.dto.VectorData;
import com.example.chatgptapp.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
// Consider @Profile("in-memory-vectorstore") for conditional bean creation later
public class InMemoryVectorStoreService implements VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorStoreService.class);

    // Using a Map to easily check for duplicate IDs and manage data
    private final Map<String, VectorData> store = new ConcurrentHashMap<>();

    @Override
    public void store(VectorData vectorData) {
        if (vectorData == null || vectorData.getId() == null || vectorData.getEmbedding() == null) {
            logger.warn("Attempted to store null or incomplete vector data. ID: {}", vectorData != null ? vectorData.getId() : "null");
            return;
        }
        if (store.containsKey(vectorData.getId())) {
            logger.info("Updating existing vector data for ID: {}", vectorData.getId());
        }
        store.put(vectorData.getId(), vectorData);
        logger.debug("Stored vector data for ID: {}. Total items in store: {}", vectorData.getId(), store.size());
    }

    @Override
    public List<VectorData> findSimilar(List<Double> queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            logger.warn("Query embedding is null or empty. Returning empty list.");
            return List.of();
        }
        if (topK <= 0) {
            logger.warn("topK is less than or equal to 0. Returning empty list.");
            return List.of();
        }

        if (store.isEmpty()) {
            logger.info("Vector store is empty. Returning empty list for similarity search.");
            return List.of();
        }

        // Calculate similarity for all items
        List<Map.Entry<VectorData, Double>> similarities = new ArrayList<>();
        for (VectorData item : store.values()) {
            if (item.getEmbedding() == null || item.getEmbedding().isEmpty()) {
                logger.warn("Skipping item with null or empty embedding. ID: {}", item.getId());
                continue;
            }
            if (item.getEmbedding().size() != queryEmbedding.size()) {
                logger.warn("Dimension mismatch between query embedding ({}) and item embedding ({}). ID: {}. Skipping.", 
                            queryEmbedding.size(), item.getEmbedding().size(), item.getId());
                continue;
            }
            double similarity = cosineSimilarity(queryEmbedding, item.getEmbedding());
            similarities.add(Map.entry(item, similarity));
        }

        // Sort by similarity in descending order and take topK
        return similarities.stream()
                .sorted(Map.Entry.<VectorData, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        store.clear();
        logger.info("In-memory vector store cleared.");
    }

    /**
     * Calculates the cosine similarity between two vectors.
     *
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return The cosine similarity, or 0.0 if inputs are invalid or norms are zero.
     */
    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size() || v1.isEmpty()) {
            logger.debug("Cosine similarity cannot be calculated for invalid inputs: v1_size={}, v2_size={}",
                         v1 != null ? v1.size() : "null", v2 != null ? v2.size() : "null");
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            logger.debug("Cosine similarity is 0 because one or both vector norms are zero.");
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

package com.example.chatgptapp.service;

import com.example.chatgptapp.dto.VectorData;
import com.example.chatgptapp.util.TextChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct; // For sample ingestion
import java.util.List;
import java.util.UUID;

@Service
public class DataIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DataIngestionService.class);

    private final OpenAIService openAIService;
    private final VectorStoreService vectorStoreService;

    // Could be made configurable via application.properties
    @Value("${data.ingestion.chunkSize:1000}") // Default chunk size in characters
    private int chunkSize;

    @Value("${data.ingestion.chunkOverlap:200}") // Default overlap in characters
    private int chunkOverlap;

    @Autowired
    public DataIngestionService(OpenAIService openAIService, VectorStoreService vectorStoreService) {
        this.openAIService = openAIService;
        this.vectorStoreService = vectorStoreService;
    }

    public void ingestText(String text, String documentId) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Text for document ID {} is null or empty. Skipping ingestion.", documentId);
            return;
        }
        logger.info("Starting ingestion for document ID: {}. Text length: {}", documentId, text.length());

        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        logger.info("Document ID {} split into {} chunks.", documentId, chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            String chunkId = documentId + "_chunk_" + String.format("%04d", i); // e.g., myDoc_chunk_0000
            logger.debug("Processing chunk ID: {}. Chunk content (first 50 chars): '{}...'", chunkId, chunkText.substring(0, Math.min(50, chunkText.length())));

            List<Double> embedding = openAIService.getEmbeddings(chunkText);

            if (embedding != null && !embedding.isEmpty()) {
                VectorData vectorData = new VectorData(chunkId, chunkText, embedding);
                vectorStoreService.store(vectorData);
                logger.info("Successfully generated embedding and stored vector data for chunk ID: {}", chunkId);
            } else {
                logger.warn("Failed to generate embedding for chunk ID: {}. Skipping storage for this chunk.", chunkId);
            }
        }
        logger.info("Finished ingestion for document ID: {}", documentId);
    }

    public void ingestTexts(List<String> texts, List<String> documentIds) {
        if (texts == null || documentIds == null || texts.size() != documentIds.size()) {
            logger.error("Texts list and document IDs list are null, or their sizes do not match. Aborting bulk ingestion.");
            return;
        }
        logger.info("Starting bulk ingestion for {} documents.", texts.size());
        for (int i = 0; i < texts.size(); i++) {
            String documentId = documentIds.get(i);
            String text = texts.get(i);
            if (documentId == null || documentId.trim().isEmpty()) {
                String generatedId = "doc-" + UUID.randomUUID().toString();
                logger.warn("Document ID at index {} was null or empty. Using generated ID: {}", i, generatedId);
                documentId = generatedId;
            }
            ingestText(text, documentId);
        }
        logger.info("Finished bulk ingestion of {} documents.", texts.size());
    }
    
    public void ingestTexts(List<String> texts) {
        if (texts == null) {
            logger.error("Texts list is null. Aborting bulk ingestion.");
            return;
        }
        logger.info("Starting bulk ingestion for {} documents with auto-generated IDs.", texts.size());
        for (int i = 0; i < texts.size(); i++) {
            String documentId = "doc-" + String.format("%04d", i) + "-" + UUID.randomUUID().toString().substring(0,8);
            ingestText(texts.get(i), documentId);
        }
        logger.info("Finished bulk ingestion of {} documents with auto-generated IDs.", texts.size());
    }

    // Optional: Sample ingestion for testing purposes
    // Remove or comment out in production if not needed via HTTP endpoint
    @PostConstruct
    public void sampleIngestion() {
        // Ensure the vector store is clean before sample ingestion if desired
        // vectorStoreService.clear(); 
        // logger.info("Cleared vector store for sample ingestion.");

        logger.info("Performing sample data ingestion on application startup...");

        String sampleText1 = "The quick brown fox jumps over the lazy dog. This is a classic sentence used to test typewriters and keyboards. It contains all letters of the English alphabet. OpenAI has developed advanced language models like GPT-3 and GPT-4.";
        String sampleText2 = "Spring Boot is a popular framework for building Java applications. It simplifies the development of stand-alone, production-grade Spring based Applications that you can 'just run'. It takes an opinionated view of the Spring platform and third-party libraries so you can get started with minimum fuss.";
        
        // Example with specific document IDs
        // List<String> textsToIngest = List.of(sampleText1, sampleText2);
        // List<String> docIdsToIngest = List.of("sampleDoc-001", "sampleDoc-002");
        // ingestTexts(textsToIngest, docIdsToIngest);

        // Example with auto-generated document IDs
        ingestText(sampleText1, "sample-doc-alpha");
        ingestText(sampleText2, "sample-doc-beta");


        // You can also test the bulk ingestion with auto-generated IDs
        // List<String> moreTexts = List.of(
        //    "Machine learning is a field of computer science that uses statistical techniques to give computer systems the ability to 'learn'.",
        //    "Artificial intelligence is intelligence demonstrated by machines, as opposed to the natural intelligence displayed by humans or animals."
        // );
        // ingestTexts(moreTexts);

        logger.info("Sample data ingestion complete.");
    }
}

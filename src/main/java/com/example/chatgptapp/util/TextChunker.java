package com.example.chatgptapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private static final Logger logger = LoggerFactory.getLogger(TextChunker.class);

    /**
     * Splits the input text into chunks of a specified size with a given overlap.
     *
     * @param text The text to chunk.
     * @param chunkSize The desired size of each chunk (in characters).
     * @param chunkOverlap The number of characters to overlap between consecutive chunks.
     * @return A list of text chunks.
     */
    public static List<String> chunkText(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isEmpty()) {
            logger.warn("Input text is null or empty. Returning empty list.");
            return List.of();
        }
        if (chunkSize <= 0) {
            logger.warn("Chunk size must be positive. Returning empty list.");
            return List.of();
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            logger.warn("Chunk overlap must be non-negative and less than chunk size. Adjusting overlap to 0 if invalid.");
            chunkOverlap = 0; // Defaulting to no overlap if configuration is invalid
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int currentPosition = 0;

        logger.debug("Starting text chunking. Total length: {}, chunkSize: {}, chunkOverlap: {}", textLength, chunkSize, chunkOverlap);

        while (currentPosition < textLength) {
            int endPosition = Math.min(currentPosition + chunkSize, textLength);
            chunks.add(text.substring(currentPosition, endPosition));
            logger.debug("Added chunk: start={}, end={}, content='{}...'", currentPosition, endPosition, text.substring(currentPosition, Math.min(currentPosition + 20, endPosition)));
            
            currentPosition += (chunkSize - chunkOverlap);
            
            // If currentPosition lands on an already processed part due to full overlap at the end, break.
            if (currentPosition >= textLength && endPosition == textLength) {
                 break;
            }
            // Safety break if overlap logic leads to no progression, though current logic should prevent this.
            if (currentPosition < endPosition && (chunkSize - chunkOverlap <= 0) && currentPosition != 0) {
                 logger.warn("Chunking logic resulted in no progression. Breaking to prevent infinite loop. chunkSize: {}, chunkOverlap: {}", chunkSize, chunkOverlap);
                 break;
            }
        }
        
        // Special case for text shorter than chunk size
        if (chunks.isEmpty() && textLength > 0) {
             chunks.add(text);
             logger.debug("Input text was shorter than chunk size, added as a single chunk.");
        }


        logger.info("Text chunking complete. Generated {} chunks.", chunks.size());
        return chunks;
    }
}

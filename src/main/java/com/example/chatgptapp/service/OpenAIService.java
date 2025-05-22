package com.example.chatgptapp.service;

import com.example.chatgptapp.dto.MessageDto;
import com.example.chatgptapp.dto.OpenAIEmbeddingRequest;
import com.example.chatgptapp.dto.OpenAIEmbeddingResponse;
import com.example.chatgptapp.dto.OpenAIChatRequest;
import com.example.chatgptapp.dto.OpenAIChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

import java.util.Collections;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private static final String OPENAI_CHAT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_CHAT_MODEL = "gpt-3.5-turbo"; // This could also be made configurable
    private static final String OPENAI_EMBEDDING_API_URL = "https://api.openai.com/v1/embeddings";

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String openAIEmbeddingModel;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.topk:3}")
    private int ragTopK;

    private final RestTemplate restTemplate;
    private final VectorStoreService vectorStoreService;

    @Autowired
    public OpenAIService(RestTemplate restTemplate, VectorStoreService vectorStoreService) {
        this.restTemplate = restTemplate;
        this.vectorStoreService = vectorStoreService;
    }

    @PostConstruct
    public void init() {
        logger.info("OpenAI API Key (first 5 chars): {}", apiKey != null && apiKey.length() > 5 ? apiKey.substring(0, 5) + "..." : "N/A");
        if (apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.warn("OpenAI API Key is not configured or is using the default placeholder. Please set openai.api.key in application.properties.");
            logger.warn("The application will likely fail to communicate with OpenAI until a valid API key is provided.");
        }
    }

    public String getOpenAIChatResponse(String userMessage) {
        if (apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.error("OpenAI API Key is not configured. Cannot make API call.");
            return "Error: OpenAI API Key is not configured. Please contact the administrator.";
        }

        String promptForChatModel = userMessage; // Default to user message

        if (ragEnabled) {
            logger.info("RAG is enabled. Attempting to retrieve context for user message.");
            List<Double> queryEmbedding = getEmbeddings(userMessage);

            if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
                logger.debug("Successfully generated embedding for user query. Embedding size: {}", queryEmbedding.size());
                List<VectorData> similarChunks = vectorStoreService.findSimilar(queryEmbedding, ragTopK);

                if (similarChunks != null && !similarChunks.isEmpty()) {
                    StringBuilder contextBuilder = new StringBuilder();
                    contextBuilder.append("Context:\n\n");
                    for (int i = 0; i < similarChunks.size(); i++) {
                        VectorData chunk = similarChunks.get(i);
                        contextBuilder.append("Chunk ").append(i + 1).append(" (ID: ").append(chunk.getId()).append("):\n");
                        contextBuilder.append(chunk.getContent());
                        if (i < similarChunks.size() - 1) {
                            contextBuilder.append("\n\n---\n\n");
                        }
                    }
                    String retrievedContext = contextBuilder.toString();
                    logger.info("Retrieved {} relevant context chunks for user query.", similarChunks.size());
                    logger.debug("Retrieved context: \n{}", retrievedContext);

                    promptForChatModel = "Based on the following context, please answer the user's query.\n\n" +
                                         retrievedContext + "\n\n" +
                                         "User Query: " + userMessage;
                    logger.debug("Augmented prompt for chat model:\n{}", promptForChatModel);

                } else {
                    logger.info("No relevant context chunks found in vector store for the user query.");
                }
            } else {
                logger.warn("Failed to generate embedding for the user query. Proceeding without RAG context.");
            }
        } else {
            logger.info("RAG is disabled. Using original user message as prompt.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        MessageDto messageDto = new MessageDto("user", promptForChatModel); // Use augmented prompt or original message
        OpenAIChatRequest requestPayload = new OpenAIChatRequest(OPENAI_CHAT_MODEL, Collections.singletonList(messageDto));

        HttpEntity<OpenAIChatRequest> entity = new HttpEntity<>(requestPayload, headers);

        try {
            logger.info("Sending request to OpenAI Chat API. URL: {}, Model: {}", OPENAI_CHAT_API_URL, OPENAI_CHAT_MODEL);
            ResponseEntity<OpenAIChatResponse> responseEntity = restTemplate.exchange(
                    OPENAI_CHAT_API_URL,
                    HttpMethod.POST,
                    entity,
                    OpenAIChatResponse.class);

            OpenAIChatResponse openAIChatResponse = responseEntity.getBody();

            if (openAIChatResponse != null && openAIChatResponse.getChoices() != null && !openAIChatResponse.getChoices().isEmpty()) {
                String assistantReply = openAIChatResponse.getChoices().get(0).getMessage().getContent();
                logger.info("Received response from OpenAI. Assistant reply: {}", assistantReply);
                return assistantReply;
            } else {
                logger.warn("Received no valid choices or empty response from OpenAI. Response body: {}", responseEntity.getBody());
                return "Error: No response from OpenAI or response was empty.";
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error communicating with OpenAI API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "Error communicating with OpenAI: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to communicate with OpenAI API: {}", e.getMessage(), e);
            return "Error: An unexpected error occurred while contacting OpenAI.";
        }
    }

    // Optional: a private method to get the API key if needed internally
    private String getApiKey() {
        return apiKey;
    }

    public List<Double> getEmbeddings(String textChunk) {
        if (apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.error("OpenAI API Key is not configured. Cannot make embedding API call.");
            return null; // Or throw new IllegalStateException("API Key not configured");
        }
        if (textChunk == null || textChunk.trim().isEmpty()) {
            logger.warn("Text chunk for embedding is null or empty. Skipping embedding generation.");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        OpenAIEmbeddingRequest requestPayload = new OpenAIEmbeddingRequest(textChunk, openAIEmbeddingModel); // Use configured model
        HttpEntity<OpenAIEmbeddingRequest> entity = new HttpEntity<>(requestPayload, headers);

        try {
            logger.info("Sending request to OpenAI Embedding API. URL: {}, Model: {}", OPENAI_EMBEDDING_API_URL, openAIEmbeddingModel);
            ResponseEntity<OpenAIEmbeddingResponse> responseEntity = restTemplate.exchange(
                    OPENAI_EMBEDDING_API_URL,
                    HttpMethod.POST,
                    entity,
                    OpenAIEmbeddingResponse.class);

            OpenAIEmbeddingResponse embeddingResponse = responseEntity.getBody();

            if (embeddingResponse != null && embeddingResponse.getData() != null && !embeddingResponse.getData().isEmpty() &&
                embeddingResponse.getData().get(0).getEmbedding() != null && !embeddingResponse.getData().get(0).getEmbedding().isEmpty()) {
                List<Double> embedding = embeddingResponse.getData().get(0).getEmbedding();
                logger.info("Successfully retrieved embedding for text chunk. Embedding size: {}", embedding.size());
                return embedding;
            } else {
                logger.warn("Received no valid embedding or empty response from OpenAI Embedding API. Response body: {}", responseEntity.getBody());
                return null;
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error communicating with OpenAI Embedding API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error when trying to communicate with OpenAI Embedding API: {}", e.getMessage(), e);
            return null;
        }
    }
}

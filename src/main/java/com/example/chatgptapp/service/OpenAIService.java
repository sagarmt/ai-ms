package com.example.chatgptapp.service;

import com.example.chatgptapp.dto.MessageDto;
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
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-3.5-turbo";

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @Autowired
    public OpenAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        MessageDto messageDto = new MessageDto("user", userMessage);
        OpenAIChatRequest requestPayload = new OpenAIChatRequest(OPENAI_MODEL, Collections.singletonList(messageDto));

        HttpEntity<OpenAIChatRequest> entity = new HttpEntity<>(requestPayload, headers);

        try {
            logger.info("Sending request to OpenAI API. URL: {}, Model: {}", OPENAI_API_URL, OPENAI_MODEL);
            ResponseEntity<OpenAIChatResponse> responseEntity = restTemplate.exchange(
                    OPENAI_API_URL,
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
}

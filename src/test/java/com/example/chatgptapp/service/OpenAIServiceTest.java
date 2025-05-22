package com.example.chatgptapp.service;

import com.example.chatgptapp.dto.ChoiceDto;
import com.example.chatgptapp.dto.MessageDto;
import com.example.chatgptapp.dto.EmbeddingDataDto;
import com.example.chatgptapp.dto.OpenAIEmbeddingRequest;
import com.example.chatgptapp.dto.OpenAIEmbeddingResponse;
import com.example.chatgptapp.dto.OpenAIChatRequest;
import com.example.chatgptapp.dto.OpenAIChatResponse;
import com.example.chatgptapp.dto.VectorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenAIServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private VectorStoreService vectorStoreService;

    @InjectMocks
    private OpenAIService openAIService;

    @Captor
    private ArgumentCaptor<HttpEntity<OpenAIChatRequest>> chatRequestCaptor;
    
    @Captor
    private ArgumentCaptor<HttpEntity<OpenAIEmbeddingRequest>> embeddingRequestCaptor;


    private final String testApiKey = "test-api-key-123";
    private final String defaultEmbeddingModel = "text-embedding-3-small";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(openAIService, "apiKey", testApiKey);
        ReflectionTestUtils.setField(openAIService, "openAIEmbeddingModel", defaultEmbeddingModel);
        ReflectionTestUtils.setField(openAIService, "ragEnabled", true); // Default to true for most tests
        ReflectionTestUtils.setField(openAIService, "ragTopK", 3);
        openAIService.init(); // Calls the @PostConstruct method
    }

    // --- Tests for getEmbeddings ---
    @Test
    void testGetEmbeddings_Success() {
        String textChunk = "Sample text for embedding.";
        List<Double> expectedEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4);
        EmbeddingDataDto embeddingDataDto = new EmbeddingDataDto("embedding", expectedEmbedding, 0);
        OpenAIEmbeddingResponse mockResponse = new OpenAIEmbeddingResponse("list", Collections.singletonList(embeddingDataDto), defaultEmbeddingModel, null);
        ResponseEntity<OpenAIEmbeddingResponse> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                contains("/embeddings"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(OpenAIEmbeddingResponse.class)))
                .thenReturn(mockResponseEntity);

        List<Double> actualEmbedding = openAIService.getEmbeddings(textChunk);

        assertEquals(expectedEmbedding, actualEmbedding);
        verify(restTemplate).exchange(contains("/embeddings"), eq(HttpMethod.POST), embeddingRequestCaptor.capture(), eq(OpenAIEmbeddingResponse.class));
        assertEquals(textChunk, embeddingRequestCaptor.getValue().getBody().getInput());
        assertEquals(defaultEmbeddingModel, embeddingRequestCaptor.getValue().getBody().getModel());
    }

    @Test
    void testGetEmbeddings_ApiError() {
        String textChunk = "Text causing API error.";
        when(restTemplate.exchange(
                contains("/embeddings"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(OpenAIEmbeddingResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request"));

        List<Double> actualEmbedding = openAIService.getEmbeddings(textChunk);
        assertNull(actualEmbedding);
    }
    
    @Test
    void testGetEmbeddings_EmptyOrNullResponse() {
        String textChunk = "Text for empty response.";
        OpenAIEmbeddingResponse mockResponse = new OpenAIEmbeddingResponse("list", Collections.emptyList(), defaultEmbeddingModel, null); // Empty data list
        ResponseEntity<OpenAIEmbeddingResponse> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAIEmbeddingResponse.class)))
                .thenReturn(mockResponseEntity);
        
        List<Double> actualEmbedding = openAIService.getEmbeddings(textChunk);
        assertNull(actualEmbedding, "Embedding should be null if API returns empty data list.");

        // Test for null embedding in data
        EmbeddingDataDto embeddingDataWithNull = new EmbeddingDataDto("embedding", null, 0);
        mockResponse.setData(Collections.singletonList(embeddingDataWithNull));
        mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
         when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAIEmbeddingResponse.class)))
                .thenReturn(mockResponseEntity);
        actualEmbedding = openAIService.getEmbeddings(textChunk);
        assertNull(actualEmbedding, "Embedding should be null if API returns null embedding list within data.");
    }

    @Test
    void testGetEmbeddings_NullOrEmptyTextChunk() {
        assertNull(openAIService.getEmbeddings(null));
        assertNull(openAIService.getEmbeddings(""));
        assertNull(openAIService.getEmbeddings("   "));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(OpenAIEmbeddingResponse.class));
    }
    
    @Test
    void testGetEmbeddings_ApiKeyNotConfigured() {
        ReflectionTestUtils.setField(openAIService, "apiKey", "YOUR_API_KEY_HERE");
        List<Double> embedding = openAIService.getEmbeddings("some text");
        assertNull(embedding);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(OpenAIEmbeddingResponse.class));
    }


    // --- Tests for getOpenAIChatResponse (Original and RAG scenarios) ---

    private void mockChatCompletionSuccess(String expectedReply) {
        MessageDto aiMessageDto = new MessageDto("assistant", expectedReply);
        ChoiceDto choiceDto = new ChoiceDto(aiMessageDto);
        OpenAIChatResponse mockOpenAIChatResponse = new OpenAIChatResponse(Collections.singletonList(choiceDto));
        ResponseEntity<OpenAIChatResponse> mockResponseEntity = new ResponseEntity<>(mockOpenAIChatResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(OpenAIChatResponse.class)))
                .thenReturn(mockResponseEntity);
    }

    @Test
    void testGetOpenAIChatResponse_Success_NoRag() {
        ReflectionTestUtils.setField(openAIService, "ragEnabled", false);
        String userMessage = "Hello, OpenAI!";
        String expectedAIReply = "Hello from mock OpenAI without RAG!";
        mockChatCompletionSuccess(expectedAIReply);

        String actualReply = openAIService.getOpenAIChatResponse(userMessage);

        assertEquals(expectedAIReply, actualReply);
        verify(restTemplate).exchange(contains("/chat/completions"), eq(HttpMethod.POST), chatRequestCaptor.capture(), eq(OpenAIChatResponse.class));
        assertEquals(userMessage, chatRequestCaptor.getValue().getBody().getMessages().get(0).getContent());
        verify(vectorStoreService, never()).findSimilar(anyList(), anyInt());
    }

    @Test
    void testGetOpenAIChatResponse_ApiKeyNotConfigured_Chat() {
        ReflectionTestUtils.setField(openAIService, "apiKey", "YOUR_API_KEY_HERE");
        String userMessage = "Test message";
        String response = openAIService.getOpenAIChatResponse(userMessage);
        assertEquals("Error: OpenAI API Key is not configured. Please contact the administrator.", response);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any());
    }
    
    @Test
    void testGetOpenAIChatResponse_HttpError_Chat() {
        ReflectionTestUtils.setField(openAIService, "ragEnabled", false); // Simplify to non-RAG for this error
        String userMessage = "Test message for HTTP error";
        when(restTemplate.exchange(
                contains("/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(OpenAIChatResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        String actualReply = openAIService.getOpenAIChatResponse(userMessage);
        assertTrue(actualReply.startsWith("Error communicating with OpenAI: 500 INTERNAL_SERVER_ERROR"), "Response should indicate an HTTP error.");
    }

    // --- RAG Scenarios for getOpenAIChatResponse ---

    @Test
    void testGetOpenAIChatResponse_RagEnabled_ContextFound() {
        String userMessage = "Tell me about Spring Boot.";
        String contextContent = "Spring Boot is a framework for Java applications.";
        String expectedAIReply = "AI response based on context.";
        List<Double> queryEmbedding = Arrays.asList(0.5, 0.4, 0.3);
        VectorData contextVector = new VectorData("doc1_chunk1", contextContent, Arrays.asList(0.51, 0.41, 0.31));

        // Mocking for RAG part
        // 1. Mock getEmbeddings for the user query
        OpenAIEmbeddingResponse embeddingResponse = new OpenAIEmbeddingResponse("list", 
            Collections.singletonList(new EmbeddingDataDto("embedding", queryEmbedding, 0)), defaultEmbeddingModel, null);
        ResponseEntity<OpenAIEmbeddingResponse> mockEmbeddingResponseEntity = new ResponseEntity<>(embeddingResponse, HttpStatus.OK);
        
        // This when is for the getEmbeddings call *within* getOpenAIChatResponse
        when(restTemplate.exchange(
                contains("/embeddings"), // Ensure this matches the embedding API URL part
                eq(HttpMethod.POST),
                embeddingRequestCaptor.capture(), // Capture the request to verify its input later
                eq(OpenAIEmbeddingResponse.class)))
                .thenReturn(mockEmbeddingResponseEntity);

        // 2. Mock vectorStoreService.findSimilar
        when(vectorStoreService.findSimilar(eq(queryEmbedding), eq(3))).thenReturn(Collections.singletonList(contextVector));
        
        // 3. Mock chat completion call
        mockChatCompletionSuccess(expectedAIReply);

        // Call the method
        String actualReply = openAIService.getOpenAIChatResponse(userMessage);

        assertEquals(expectedAIReply, actualReply);

        // Verify embedding request for user query
        assertEquals(userMessage, embeddingRequestCaptor.getValue().getBody().getInput());

        // Verify findSimilar was called
        verify(vectorStoreService).findSimilar(eq(queryEmbedding), eq(3));

        // Verify chat completion request contains augmented prompt
        verify(restTemplate).exchange(contains("/chat/completions"), eq(HttpMethod.POST), chatRequestCaptor.capture(), eq(OpenAIChatResponse.class));
        String augmentedPrompt = chatRequestCaptor.getValue().getBody().getMessages().get(0).getContent();
        assertTrue(augmentedPrompt.contains(contextContent), "Augmented prompt should contain context.");
        assertTrue(augmentedPrompt.contains(userMessage), "Augmented prompt should contain user query.");
        assertTrue(augmentedPrompt.startsWith("Based on the following context"), "Augmented prompt should start with specific instruction.");
    }

    @Test
    void testGetOpenAIChatResponse_RagEnabled_NoContextFound() {
        String userMessage = "Unknown topic for RAG.";
        String expectedAIReply = "AI response without context.";
        List<Double> queryEmbedding = Arrays.asList(0.8, 0.7, 0.6);

        // Mock getEmbeddings for user query
        OpenAIEmbeddingResponse embeddingResponse = new OpenAIEmbeddingResponse("list", 
            Collections.singletonList(new EmbeddingDataDto("embedding", queryEmbedding, 0)), defaultEmbeddingModel, null);
        ResponseEntity<OpenAIEmbeddingResponse> mockEmbeddingResponseEntity = new ResponseEntity<>(embeddingResponse, HttpStatus.OK);
         when(restTemplate.exchange(
                contains("/embeddings"), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAIEmbeddingResponse.class)))
                .thenReturn(mockEmbeddingResponseEntity);
        
        // Mock vectorStoreService to return empty list
        when(vectorStoreService.findSimilar(eq(queryEmbedding), eq(3))).thenReturn(Collections.emptyList());
        
        mockChatCompletionSuccess(expectedAIReply);

        String actualReply = openAIService.getOpenAIChatResponse(userMessage);
        assertEquals(expectedAIReply, actualReply);

        // Verify that the prompt sent to chat completion is the original user message
        verify(restTemplate).exchange(contains("/chat/completions"), eq(HttpMethod.POST), chatRequestCaptor.capture(), eq(OpenAIChatResponse.class));
        assertEquals(userMessage, chatRequestCaptor.getValue().getBody().getMessages().get(0).getContent());
    }

    @Test
    void testGetOpenAIChatResponse_RagEnabled_QueryEmbeddingFails() {
        String userMessage = "Query whose embedding will fail.";
        String expectedAIReply = "AI response when embedding fails.";

        // Mock getEmbeddings to return null (simulating failure)
        when(restTemplate.exchange(
                contains("/embeddings"), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAIEmbeddingResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)); // Or return response that leads to null

        mockChatCompletionSuccess(expectedAIReply);

        String actualReply = openAIService.getOpenAIChatResponse(userMessage);
        assertEquals(expectedAIReply, actualReply);

        // Verify vectorStoreService.findSimilar is NOT called
        verify(vectorStoreService, never()).findSimilar(anyList(), anyInt());
        
        // Verify prompt is original user message
        verify(restTemplate).exchange(contains("/chat/completions"), eq(HttpMethod.POST), chatRequestCaptor.capture(), eq(OpenAIChatResponse.class));
        assertEquals(userMessage, chatRequestCaptor.getValue().getBody().getMessages().get(0).getContent());
    }

    @Test
    void testGetOpenAIChatResponse_RagDisabled() {
        ReflectionTestUtils.setField(openAIService, "ragEnabled", false);
        String userMessage = "Simple query with RAG disabled.";
        String expectedAIReply = "AI response with RAG disabled.";
        mockChatCompletionSuccess(expectedAIReply);

        String actualReply = openAIService.getOpenAIChatResponse(userMessage);
        assertEquals(expectedAIReply, actualReply);

        // Verify getEmbeddings (for user query) and findSimilar are NOT called
        verify(restTemplate, never()).exchange(contains("/embeddings"), any(), any(), any()); // no embedding call
        verify(vectorStoreService, never()).findSimilar(anyList(), anyInt());
        
        // Verify prompt is original user message
        verify(restTemplate).exchange(contains("/chat/completions"), eq(HttpMethod.POST), chatRequestCaptor.capture(), eq(OpenAIChatResponse.class));
        assertEquals(userMessage, chatRequestCaptor.getValue().getBody().getMessages().get(0).getContent());
    }

    // Test for original testGetOpenAIChatResponse_EmptyChoices (Non-RAG)
    @Test
    void testGetOpenAIChatResponse_EmptyChoices_NoRag() {
        ReflectionTestUtils.setField(openAIService, "ragEnabled", false);
        String userMessage = "Hello, OpenAI!";
        OpenAIChatResponse mockOpenAIChatResponse = new OpenAIChatResponse(Collections.emptyList());
        ResponseEntity<OpenAIChatResponse> mockResponseEntity = new ResponseEntity<>(mockOpenAIChatResponse, HttpStatus.OK);

        when(restTemplate.exchange(contains("/chat/completions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAIChatResponse.class)))
                .thenReturn(mockResponseEntity);

        String actualReply = openAIService.getOpenAIChatResponse(userMessage);
        assertEquals("Error: No response from OpenAI or response was empty.", actualReply);
    }

    // Test for original testGetOpenAIChatResponse_NullMessageInChoice (Non-RAG)
    @Test
    void testGetOpenAIChatResponse_NullMessageInChoice_NoRag() {
        ReflectionTestUtils.setField(openAIService, "ragEnabled", false);
        String userMessage = "Hello, OpenAI!";
        MessageDto nullContentMessage = new MessageDto("assistant", null);
        ChoiceDto choiceWithNullContent = new ChoiceDto(nullContentMessage);
        OpenAIChatResponse responseWithNullContent = new OpenAIChatResponse(Collections.singletonList(choiceWithNullContent));
        ResponseEntity<OpenAIChatResponse> responseEntityWithNullContent = new ResponseEntity<>(responseWithNullContent, HttpStatus.OK);

        when(restTemplate.exchange(contains("/chat/completions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(OpenAIChatResponse.class)))
                .thenReturn(responseEntityWithNullContent);
        
        String actualReply = openAIService.getOpenAIChatResponse(userMessage);
        assertNull(actualReply); // If content is null, service returns null
    }
}

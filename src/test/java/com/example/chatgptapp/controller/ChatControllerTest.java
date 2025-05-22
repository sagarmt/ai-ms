package com.example.chatgptapp.controller;

import com.example.chatgptapp.service.OpenAIService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAIService openAIService;

    @Test
    public void testChatEndpoint_Success() throws Exception {
        String userMessage = "Hello, AI!";
        String aiResponse = "Test AI response from service";

        when(openAIService.getOpenAIChatResponse(userMessage)).thenReturn(aiResponse);

        mockMvc.perform(post("/api/v1/chat/") // Ensure trailing slash if that's how it's mapped
                .contentType(MediaType.TEXT_PLAIN) // Assuming the controller accepts plain text for the message
                .content(userMessage))
                .andExpect(status().isOk())
                .andExpect(content().string(aiResponse));
    }

    @Test
    public void testChatEndpoint_ServiceReturnsError() throws Exception {
        String userMessage = "Another message";
        String serviceErrorResponse = "Error: Service unavailable";

        when(openAIService.getOpenAIChatResponse(userMessage)).thenReturn(serviceErrorResponse);

        mockMvc.perform(post("/api/v1/chat/")
                .contentType(MediaType.TEXT_PLAIN)
                .content(userMessage))
                .andExpect(status().isOk()) // The controller itself should still return 200 OK
                .andExpect(content().string(serviceErrorResponse));
    }
}

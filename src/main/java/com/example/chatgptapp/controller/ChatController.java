package com.example.chatgptapp.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.chatgptapp.service.OpenAIService;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final OpenAIService openAIService;

    @Autowired
    public ChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/")
    public String chat(@RequestBody String message) {
        return openAIService.getOpenAIChatResponse(message);
    }
}

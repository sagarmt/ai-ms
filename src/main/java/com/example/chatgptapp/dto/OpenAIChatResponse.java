package com.example.chatgptapp.dto;

import java.util.List;

public class OpenAIChatResponse {
    private List<ChoiceDto> choices;

    public OpenAIChatResponse() {
    }

    public OpenAIChatResponse(List<ChoiceDto> choices) {
        this.choices = choices;
    }

    public List<ChoiceDto> getChoices() {
        return choices;
    }

    public void setChoices(List<ChoiceDto> choices) {
        this.choices = choices;
    }
}

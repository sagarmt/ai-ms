package com.example.chatgptapp.dto;

public class ChoiceDto {
    private MessageDto message;

    public ChoiceDto() {
    }

    public ChoiceDto(MessageDto message) {
        this.message = message;
    }

    public MessageDto getMessage() {
        return message;
    }

    public void setMessage(MessageDto message) {
        this.message = message;
    }
}

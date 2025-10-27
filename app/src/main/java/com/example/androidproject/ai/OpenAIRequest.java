package com.example.androidproject.ai;

import java.util.ArrayList;
import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private double temperature;

    public OpenAIRequest(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
        this.temperature = 0.7;
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public String getModel() { return model; }
    public List<Message> getMessages() { return messages; }
    public double getTemperature() { return temperature; }
}

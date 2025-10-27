package com.example.androidproject.ai;

import java.util.List;

public class OpenAIResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() { return choices; }

    public static class Choice {
        private Message message;
        public Message getMessage() { return message; }
    }

    public static class Message {
        private String content;
        public String getContent() { return content; }
    }
}

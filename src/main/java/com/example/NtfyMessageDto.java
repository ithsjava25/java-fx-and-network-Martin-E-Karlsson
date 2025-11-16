package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NtfyMessageDto(String id, long time, String event, String topic, String message, String user) {

    public NtfyMessageDto {
        if (message != null && message.trim().startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(message);

                String parsedUser;
                String parsedMessage;

                if (node.has("message")) {
                    parsedMessage = node.get("message").asText();
                } else {
                    parsedMessage = message;
                }

                if (node.has("user")) {
                    parsedUser = node.get("user").asText();
                } else {
                    parsedUser = user;
                }

                message = parsedMessage;
                user = parsedUser;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public String getMessage() {
        return message;
    }

    public String getUser() {
        return user;
    }
}
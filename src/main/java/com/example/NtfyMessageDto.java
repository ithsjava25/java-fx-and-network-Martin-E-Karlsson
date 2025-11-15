package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

                String parsedMessage = node.has("message") ? node.get("message").asText() : message;
                String parsedUser = node.has("user") ? node.get("user").asText() : user;

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

    public LocalTime getTime() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(time),
                ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }
}
package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


/**
 * Model layer: encapsulates application data and business logic.
 * Notes:
 * - This class is now testable: you can inject HttpClient, hostName, and ObjectMapper.
 */
public class ChatModel {

    private final HttpClient http;
    private final String hostName;
    private final ObjectMapper mapper;

    private final String username;
    private String topic;

    private final ObservableList<NtfyMessageDto> messages = FXCollections.observableArrayList();
    private final StringProperty messageToSend = new SimpleStringProperty();

    /**
     * Production-style constructor: reads HOST_NAME from .env and uses default HttpClient/ObjectMapper.
     * Does not start receive loop automatically (autoReceive=false). Call receiveMessage() to start.
     */
    public ChatModel(String username, String topic) {
        this(username, topic, HttpClient.newHttpClient(), loadHostFromDotenv(), new ObjectMapper(), false);
        receiveMessage();
    }

    /**
     * Testable constructor: inject dependencies. Set autoReceive to true to start receiveMessage() immediately.
     */
    public ChatModel(String username,
                     String topic,
                     HttpClient httpClient,
                     String hostName,
                     ObjectMapper mapper,
                     boolean autoReceive) {
        this.username = username;
        this.topic = topic;
        this.http = httpClient;
        this.hostName = Objects.requireNonNull(hostName);
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
        if (autoReceive) {
            receiveMessage();
        }
    }

    private static String loadHostFromDotenv() {
        Dotenv dotenv = Dotenv.load();
        return Objects.requireNonNull(dotenv.get("HOST_NAME"));
    }

    public ObservableList<NtfyMessageDto> getMessages() {
        return messages;
    }

    public String getUsername() {
        return username;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessageToSend() {
        return messageToSend.get();
    }

    public StringProperty messageToSendProperty() {
        return messageToSend;
    }

    public void setMessageToSend(String message) {
        messageToSend.set(message);
    }

    /**
     * Send a message to the configured host/topic using the JSON payload format.
     */
    public void sendMessage() {
        String message = messageToSend.get();
        if (message == null) message = "";
        String jsonPayload = String.format(
                "{ \"message\": \"%s\", \"user\": \"%s\" }",
                escapeForJson(message), escapeForJson(username)
        );
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .header("Content-Type", "application/json")
                .uri(URI.create(hostName + "/" + topic))
                .build();
        try {
            var response = http.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted sending message");
        }
    }

    private static String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public CompletableFuture<Void> receiveMessage() {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(hostName + "/" + topic + "/json"))
                .build();
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body()
                        .map(s -> {
                            try {
                                return mapper.readValue(s, NtfyMessageDto.class);
                            } catch (Exception e) {
                                System.out.println("Failed to parse incoming line: " + e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .filter(message -> {
                            try {
                                return "message".equals(message.event());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .peek(m -> System.out.println("Received message: " + m))
                        .forEach(m -> Platform.runLater(() -> messages.add(m))));
    }


}
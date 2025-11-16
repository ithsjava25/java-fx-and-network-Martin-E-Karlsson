package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final String topic;
    private String imagePath;

    private final ObservableList<NtfyMessageDto> messages = FXCollections.observableArrayList();
    private final StringProperty messageToSend = new SimpleStringProperty();

    record MessagePayload(String message, String user) {}

    /**
     * Production-style constructor: reads HOST_NAME from .env and uses default HttpClient/ObjectMapper.
     * Does not start receive loop automatically (autoReceive=false). Call receiveMessage() to start.
     * @param username The username provided by login client.
     * @param topic The topic to use provided by login client.
     */
    public ChatModel(String username, String topic) {
        this(username, topic, HttpClient.newHttpClient(), loadHostFromDotenv(), new ObjectMapper(), true);
    }

    /**
     * Used for testing and dependency injection.
     * Testable constructor: inject dependencies. Set autoReceive to true to start receiveMessage() immediately.
     * @param username The username to use.
     * @param topic The topic to use.
     * @param httpClient The HttpClient to use.
     * @param hostName The host name (base URL) to use.
     * @param mapper The ObjectMapper to use.
     * @param autoReceive Whether to start receiving messages immediately.
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

    /**
     * Load the HOST_NAME from the .env file using Dotenv library.
     * @return The host name as a string.
     */
    private static String loadHostFromDotenv() {
        try {
            Dotenv dotenv = Dotenv.load();
            String host = dotenv.get("HOST_NAME");
            if (host == null) {
                    throw new IllegalStateException("HOST_NAME not found in .env file");
                }
            return host;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load configuration from .env: " + e.getMessage(), e);
        }
    }

    /**
     * Get the observable list of messages.
     * @return The observable list of NtfyMessageDto.
     */
    public ObservableList<NtfyMessageDto> getMessages() {
        return messages;
    }

    /**
     * Get the username.
     * @return The username string.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the topic.
     * @return The topic string.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Set the message to send.
     * @param message The message string.
     */
    public void setMessageToSend(String message) {
        messageToSend.set(message);
    }

    /**
     * Send a message to the configured host/topic using the JSON payload format.
     */
    public CompletableFuture<Object> sendMessage() {
        String message = messageToSend.get();
        if (message == null) message = "";
        String jsonPayload = formatJsonPayload(message);
        HttpRequest httpRequest = buildHttpRequest(jsonPayload);
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> null)
                .exceptionally(ex -> {
                    System.out.println("Error sending message: " + ex.getMessage());
                    return null;
                });
    }


    /**
     * Format the message and username into a JSON payload string.
     * @param message The message string.
     * @return The formatted JSON payload string.
     */
    private String formatJsonPayload(String message) {
        return String.format(
                "{ \"message\": \"%s\", \"user\": \"%s\" }",
                escapeForJson(message), escapeForJson(username)
        );
    }

    /**
     * Build the HTTP POST request with the given JSON payload.
     * @param jsonPayload The JSON payload string.
     * @return The constructed HttpRequest.
     */
    private HttpRequest buildHttpRequest(String jsonPayload) {
        return HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .header("Content-Type", "application/json")
                .uri(URI.create(hostName + "/" + topic))
                .build();
    }

    /**
     * Escape special characters in a string for safe inclusion in JSON.
     * @param s The input string.
     * @return The escaped string.
     */
    private static String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    /**
     * Receive messages from the configured host/topic.
     * Parses newline-delimited JSON lines and adds "message" events to the messages list.
     * @return A CompletableFuture that completes when the receive operation is done. (Used for testing.)
     */
    public CompletableFuture<Void> receiveMessage() {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(hostName + "/" + topic + "/json"))
                .build();
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        System.out.println("Unexpected response status: " + response.statusCode());
                        return;
                    }
                    response.body()
                        .map(s -> s == null ? "" : s.trim())
                        .filter(s -> !s.isEmpty())
                        .map(s -> {
                            try {
                                return mapper.readValue(s, NtfyMessageDto.class);
                            } catch (Exception e) {
                                System.out.println("Failed to parse incoming line: " + e.getMessage());
                                return null;
                            }
                        }
                    )
                        .filter(Objects::nonNull)
                        .filter(message -> {
                            try {
                                return "message".equals(message.event());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                .forEach(m -> Platform.runLater(() -> messages.add(m)));
                });
    }

    /**
     * * Sets the image path to send. (Placeholder for future implementation)
     * @param imagePath The path of the image to send.
     */
    public void setImageToSend(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Send an image to the configured host/topic.
     * (Not fully implemented feature)
     */
    public void sendImage() {
        // TODO: Image sending logic
    }
}
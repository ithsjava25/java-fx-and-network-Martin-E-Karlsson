package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

/**
 * Model layer: encapsulates application data and business logic.
 */
public class ChatModel {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String hostName;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String username;
    private String topic;

    private final ObservableList<NtfyMessageDto> messages = FXCollections.observableArrayList();
    private final StringProperty messageToSend = new SimpleStringProperty();

    public ChatModel(String username, String topic) {
        this.username = username;
        this.topic = topic;
        Dotenv dotenv = Dotenv.load();
        hostName = Objects.requireNonNull(dotenv.get("HOST_NAME"));
        receiveMessage();
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

    public void sendMessage() {
        String message = messageToSend.get();
        String jsonPayload = String.format(
                "{ \"message\": \"%s\", \"user\": \"%s\" }",
                message, username
        );
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .header("Content-Type", "application/json")
//                .header("Cache", "no")
                .uri(URI.create(hostName + "/" + topic))
                .build();
        try {
            //Todo: handle long blocking send requests to not freeze the JavaFX thread
            //1. Use thread send message?
            //2. Use async?
            var response = http.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            System.out.println("Error sending message");
        } catch (InterruptedException e) {
            System.out.println("Interrupted sending message");
        }
    }

    public void receiveMessage() {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(hostName + "/" + topic + "/json"))
                .build();

        http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body()
                        .map(s ->
                                mapper.readValue(s, NtfyMessageDto.class))
                        .filter(message -> message.event().equals("message"))
                        .peek(System.out::println)
                        .forEach(m -> Platform.runLater(() -> messages.add(m))));
    }


}
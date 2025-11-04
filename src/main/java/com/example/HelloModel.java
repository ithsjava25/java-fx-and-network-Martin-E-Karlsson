package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
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
public class HelloModel {
    private final String hostName;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final ObservableList<NtfyMessageDto> messages = FXCollections.observableArrayList();

    public HelloModel() {
        Dotenv dotenv = Dotenv.load();
        hostName = Objects.requireNonNull(dotenv.get("HOST_NAME"));
        receiveMessage();
    }

    public ObservableList<NtfyMessageDto> getMessages() {
        return messages;
    }

    /**
     * Returns a greeting based on the current Java and JavaFX versions.
     */
    public String getGreeting() {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        return "Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".";
    }

    public void sendMessage() {
        //Todo: Handles long blocking send requests to not freeze the JavaFX thread
        //1. Use thread send message?
        //2. Use async?

        HttpRequest httpRequest = HttpRequest.newBuilder().
                POST(HttpRequest.BodyPublishers.ofString("Hello world 🐼"))
                .uri(URI.create(hostName + "/mytopic"))
                .build();
        try {
            var response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            System.out.println("Error sending message");
        } catch (InterruptedException e) {
            System.out.println("Interrupted sending message");
        }
    }

    public void receiveMessage() {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(hostName + "/mytopic.json"))
                .build();

        http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body()
                        .map(s ->
                                mapper.readValue(s, NtfyMessageDto.class))
                        .filter(message -> message.event().equals("message"))
                        .peek(System.out::println)
                        .forEach(msg ->
                                Platform.runLater(()->messages.add(msg))));
    }
}


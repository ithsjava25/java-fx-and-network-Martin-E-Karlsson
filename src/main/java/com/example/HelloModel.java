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
public class HelloModel {
    private final NtfyConnection connection;
    private final String hostName;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();


    private final ObservableList<NtfyMessageDto> messages = FXCollections.observableArrayList();
    private final StringProperty messagesToSend = new SimpleStringProperty();

    public HelloModel(NtfyConnection connection) {
        Dotenv dotenv = Dotenv.load();
        hostName = Objects.requireNonNull(dotenv.get("HOST_NAME"));
        receiveMessage();
        this.connection = connection;
    }

    public ObservableList<NtfyMessageDto> getMessages() {
        return messages;
    }

    public String getMessagesToSend() {
        return messagesToSend.get();
    }

    public StringProperty messagesToSendProperty() {
        return messagesToSend;
    }


    public void setMessageToSend(String message){
        messagesToSend.set(message);
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
//        connection.send(messagesToSend.get());
        HttpRequest httpRequest = HttpRequest.newBuilder().
                POST(HttpRequest.BodyPublishers.ofString("Hello world 🐼"))
                .header("Cache", "no")
                .uri(URI.create(hostName + "/mytopic"))
                .build();
        try {
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


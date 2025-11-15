package com.example;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;

import java.net.http.HttpClient;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


@WireMockTest
class ChatModelTest {

    @BeforeAll
    public static void initJavaFx() throws Exception {
        // Starts JavaFX toolkit. If it is already started the try-statement is ignored.
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            // A brief delay to ensure startup completes
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX Platform.startup timed out");
            }
        } catch (IllegalStateException ex) {
            // The JavaFX platform is already running
        }
    }

    @Test
    @DisplayName("sendMessage posts JSON with message and user to the topic endpoint")
    void sendMessageToFakeServer(WireMockRuntimeInfo wmRuntimeInfo) {
        // Setup to create a ChatModel pointing to WireMock server
        String host = "http://localhost:" + wmRuntimeInfo.getHttpPort();
        HttpClient httpClient = HttpClient.newHttpClient();
        var model = new ChatModel("TestUser", "testTopic", httpClient, host, new ObjectMapper(), false);

        // stub WireMock for POST
        stubFor(post(urlEqualTo("/testTopic")).willReturn(ok()));

        // Act
        model.setMessageToSend("Hello World");
        model.sendMessage();

        // Assert: verify the JSON body contains the message and user fields
        verify(postRequestedFor(urlEqualTo("/testTopic"))
                .withRequestBody(matching("(?s).*\"message\"\\s*:\\s*\"Hello World\".*"))
                .withRequestBody(matching("(?s).*\"user\"\\s*:\\s*\"TestUser\".*"))
        );
    }

    @Test
    @DisplayName("receiveMessage parses newline-delimited JSON lines and adds message events")
    void receiveMessageFromFakeServer(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // Setup to create a ChatModel pointing to WireMock server
        String host = "http://localhost:" + wmRuntimeInfo.getHttpPort();
        HttpClient httpClient = HttpClient.newHttpClient();
        var model = new ChatModel("TestUser", "testTopic", httpClient, host, new ObjectMapper(), false);

        // Stub WireMock for GET with two JSON lines representing messages with inserted newlines
        String line1 = "{\"event\":\"message\",\"message\":\"Hello\",\"user\":\"Alice\"}";
        String line2 = "{\"event\":\"message\",\"message\":\"Second\",\"user\":\"Bob\"}";

        stubFor(get(urlEqualTo("/testTopic/json"))
                .willReturn(aResponse().withStatus(200).withBody(line1 + "\n" + line2)));

        // Act: start receive and wait for completion
        var future = model.receiveMessage();

        // Safely wait for async to finish
        future.get(2, TimeUnit.SECONDS);

        // Run delay loop to allow messages to be processed
        long start = System.currentTimeMillis();
        while (model.getMessages().size() < 2 && System.currentTimeMillis() - start < 2000) {
            Thread.sleep(50);
        }

        // Assert
        assertThat(model.getMessages()).hasSize(2);
        var first = model.getMessages().get(0);
        assertThat(first.event()).isEqualTo("message");
        assertThat(first.message()).isEqualTo("Hello");
    }
}
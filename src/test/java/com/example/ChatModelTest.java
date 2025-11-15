package com.example;

//import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


@WireMockTest
class ChatModelTest {

    @Test
    @DisplayName("sendMessage posts JSON with message and user to the topic endpoint")
    void sendMessageToFakeServer(WireMockRuntimeInfo wmRuntimeInfo) {
        // Arrange
        String host = "http://localhost:" + wmRuntimeInfo.getHttpPort();
        HttpClient httpClient = HttpClient.newHttpClient();
        var model = new ChatModel("TestUser", "testTopic", httpClient, host, new ObjectMapper(), false);

        // stub WireMock for POST (not strictly required to return anything special)
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
        // Arrange
        String host = "http://localhost:" + wmRuntimeInfo.getHttpPort();
        HttpClient httpClient = HttpClient.newHttpClient();
        var model = new ChatModel("TestUser", "testTopic", httpClient, host, new ObjectMapper(), false);

        // Prepare two newline-separated JSON lines (as ntfy /json streaming might produce)
        String line1 = "{\"event\":\"message\",\"message\":\"Hello\",\"user\":\"Alice\"}";
        String line2 = "{\"event\":\"message\",\"message\":\"Second\",\"user\":\"Bob\"}";

        stubFor(get(urlEqualTo("/testTopic/json"))
                .willReturn(aResponse().withStatus(200).withBody(line1 + "\n" + line2)));

        // Act: start receive and wait for completion
        var future = model.receiveMessage();

        // Wait for the async chain to complete (timeout safety)
        future.get(2, TimeUnit.SECONDS);

        // The receiveMessage result schedules additions on the JavaFX Application thread;
        // in unit tests (without a JavaFX thread) the Platform.runLater() will enqueue tasks
        // and they may not run. For simplicity in headless unit tests, you can either:
        //  - Replace Platform.runLater(...) in ChatModel with direct addition when Platform.isFxApplicationThread() is false,
        //  - Or check for presence with a short polling loop. Here we poll until items appear.
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
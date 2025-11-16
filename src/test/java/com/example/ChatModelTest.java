package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Combined test class that contains:
 * - WireMock integration tests (annotated with @WireMockTest)
 * - Mockito-based unit tests for private helpers (via reflection) using MockitoExtension
 *
 * Notes:
 * - JavaFX Platform is started once in @BeforeAll.
 * - @AfterEach resets WireMock requests to avoid cross-test interference.
 * - Unit tests use a mocked HttpClient to confirm helpers don't call the client.
 */
@WireMockTest
@ExtendWith(MockitoExtension.class)
class ChatModelTest {

    @Mock
    HttpClient mockHttpClient;

    private ChatModel chatModelWithMock;

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        // Make tests and application code use the WireMock base URL as HOST_NAME
        System.setProperty("HOST_NAME", wireMock.baseUrl());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
        System.clearProperty("HOST_NAME");
    }

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
    }

    @BeforeAll
    public static void initJavaFx() throws Exception {
        // Starts JavaFX toolkit once for tests that use Platform.runLater.
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX Platform.startup timed out");
            }
        } catch (IllegalStateException ex) {
            // Platform already started; ignore, but re-throw if it's not that case
            if (!ex.getMessage().contains("Toolkit already initialized")) {
                throw ex;
            }
        }
    }

    @BeforeEach
    public void setUp() {
        // Create a ChatModel for unit tests that uses the mocked HttpClient.
        // Use a deterministic hostName and ObjectMapper; autoReceive=false to avoid network activity.
        chatModelWithMock = new ChatModel("user\"name\\with", "mytopic", mockHttpClient, "https://example.com", new ObjectMapper(), false);
    }

    @AfterEach
    void cleanupWireMock() {
        // Reset recorded requests (and optionally stubs) so WireMock state doesn't leak across tests.
        resetAllRequests();
    }

    //
    // Mockito-based unit tests for helpers (using reflection because methods are private)
    //

    @Test
    public void testEscapeForJson_nullAndSpecialChars() throws Exception {
        Method escapeMethod = ChatModel.class.getDeclaredMethod("escapeForJson", String.class);
        escapeMethod.setAccessible(true);

        // null -> empty string
        String resNull = (String) escapeMethod.invoke(null, (Object) null);
        assertEquals("", resNull);

        // backslash and quote should be escaped
        String input = "Line1\\Line2 \"quoted\"";
        String expected = "Line1\\\\Line2 \\\"quoted\\\"";
        String res = (String) escapeMethod.invoke(null, input);
        assertEquals(expected, res);

        // verify mock not used by these private helper invocations
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    public void testFormatJsonPayload_escapesMessageAndUsername() throws Exception {
        Method formatMethod = ChatModel.class.getDeclaredMethod("formatJsonPayload", String.class);
        formatMethod.setAccessible(true);

        // message with characters that need escaping
        String msg = "Hello \"world\" \\ test";
        String payload = (String) formatMethod.invoke(chatModelWithMock, msg);

        // username from setUp is: user"name\with -> should be escaped in JSON
        String expectedMessageEscaped = "Hello \\\"world\\\" \\\\ test";
        String expectedUserEscaped = "user\\\"name\\\\with";
        String expectedJson = String.format("{ \"message\": \"%s\", \"user\": \"%s\" }",
                expectedMessageEscaped, expectedUserEscaped);

        assertEquals(expectedJson, payload);

        // verify mock not used by this private helper
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    public void testBuildHttpRequest_containsCorrectMethodHeaderUriAndBody() throws Exception {
        Method buildMethod = ChatModel.class.getDeclaredMethod("buildHttpRequest", String.class);
        buildMethod.setAccessible(true);

        String jsonPayload = "{ \"message\": \"hi\", \"user\": \"u\" }";
        HttpRequest req = (HttpRequest) buildMethod.invoke(chatModelWithMock, jsonPayload);

        // method should be POST
        assertEquals("POST", req.method());

        // URI should be host + "/" + topic
        assertEquals("https://example.com/mytopic", req.uri().toString());

        // Content-Type header should be application/json
        Optional<String> ct = req.headers().firstValue("Content-Type");
        assertTrue(ct.isPresent());
        assertEquals("application/json", ct.get());

        // Ensure body publisher exists and contains the payload text
        Optional<BodyPublisher> maybePublisher = req.bodyPublisher();
        assertTrue(maybePublisher.isPresent(), "Expected body publisher to be present for POST request");

        BodyPublisher publisher = maybePublisher.get();

        // Read the BodyPublisher by subscribing to it and collecting ByteBuffers.
        String body = readBodyPublisherToString(publisher);
        assertEquals(jsonPayload, body);

        // confirm the mock http client was not called during construction of request
        verifyNoInteractions(mockHttpClient);
    }

    // Helper to read BodyPublisher content into a String (blocking with timeout).
    private static String readBodyPublisherToString(BodyPublisher publisher) throws Exception {
        CompletableFuture<String> done = new CompletableFuture<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Flow.Subscriber<ByteBuffer> subscriber = new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] b = new byte[item.remaining()];
                item.get(b);
                try {
                    out.write(b);
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                done.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                done.complete(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        };

        publisher.subscribe(subscriber);

        // wait up to 1 second for body to be collected
        return done.get(1, TimeUnit.SECONDS);
    }

    //
    // WireMock integration tests
    //

    /**
     * Integration Test checking if sendMessage sends the correct JSON payload to the server.
     */
    @Test
    @DisplayName("sendMessage posts JSON with message and user to the topic endpoint")
    void sendMessageToFakeServer(WireMockRuntimeInfo wmRuntimeInfo) throws ExecutionException, InterruptedException, TimeoutException {
        // Setup ChatModel pointing to the WireMock server
        String host = "http://localhost:" + wmRuntimeInfo.getHttpPort();
        HttpClient httpClient = HttpClient.newHttpClient();
        var model = new ChatModel("TestUser", "testTopic", httpClient, host, new ObjectMapper(), false);

        // stub WireMock for POST
        stubFor(post(urlEqualTo("/testTopic")).willReturn(ok()));

        // Act
        model.setMessageToSend("Hello World");
        var future = model.sendMessage();
        future.get(2, TimeUnit.SECONDS);  // Wait for async completion

        // Assert: verify the JSON body contains the message and user fields
        verify(postRequestedFor(urlEqualTo("/testTopic"))
                .withRequestBody(matching("(?s).*\"message\"\\s*:\\s*\"Hello World\".*"))
                .withRequestBody(matching("(?s).*\"user\"\\s*:\\s*\"TestUser\".*"))
        );
    }

    /**
     * Integration Test checking if receiveMessage correctly parses newline-delimited JSON lines.
     */
    @Test
    @DisplayName("receiveMessage parses newline-delimited JSON lines and adds message events")
    void receiveMessageFromFakeServer(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // Setup ChatModel pointing to WireMock server
        String host = "http://localhost:" + wmRuntimeInfo.getHttpPort();
        HttpClient httpClient = HttpClient.newHttpClient();
        var model = new ChatModel("TestUser", "testTopic", httpClient, host, new ObjectMapper(), false);

        // Stub WireMock for GET with two JSON lines representing messages separated by newline
        String line1 = "{\"event\":\"message\",\"message\":\"Hello\",\"user\":\"Alice\"}";
        String line2 = "{\"event\":\"message\",\"message\":\"Second\",\"user\":\"Bob\"}";

        stubFor(get(urlEqualTo("/testTopic/json"))
                .willReturn(aResponse().withStatus(200).withBody(line1 + "\n" + line2)));

        // Act: start receive and wait for completion
        var future = model.receiveMessage();

        // Wait for the async HTTP call to complete (timeout protects the test)
        future.get(2, TimeUnit.SECONDS);

        // Allow a brief window for Platform.runLater to add messages to the observable list
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
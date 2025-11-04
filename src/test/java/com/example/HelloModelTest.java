package com.example;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest
class HelloModelTest {

    @Test
    @DisplayName("When calling send sendMessage it should call connection send")
    void sendMessageCallsConnectionWithMessageToSend() {
        // Arrange Given
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend("Hello World");
        // Act When
        model.sendMessage();
        // Assert Then
        assertThat(spy.message).isEqualTo("Hello World");
    }

    @Test
    void sendMessageToFakeServer(WireMockRuntimeInfo wmRuntimeInfo) {
        var con = new NtfyConnectionImpl("http://localhost:" + wmRuntimeInfo.getHttpPort());
        var model = new HelloModel(con);
        model.setMessageToSend(("Hello World"));
        stubFor(post("/mytopic").willReturn(ok()));

        model.sendMessage();

        verify((postRequestedFor(urlEqualTo("/mytopic"))
                .withRequestBody(matching("Hello World"))));
    }

}
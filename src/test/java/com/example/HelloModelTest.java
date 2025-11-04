package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

}
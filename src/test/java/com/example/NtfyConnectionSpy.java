package com.example;

import java.util.function.Consumer;

public class NtfyConnectionSpy implements NtfyConnection{
    String message;

    @Override
    public boolean send(String message) {
        this.message = message;
        return true;
    }

    @Override
    public void receive(Consumer<NtfyMessageDto> messageHandler) {

    }
}

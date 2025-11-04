package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record NtfyMessageDto(String id, long time, String event, String topic, String message){}
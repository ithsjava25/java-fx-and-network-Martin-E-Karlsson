package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    public TextField usernameField;

    @FXML
    public Label errorLabel;

    @FXML
    public TextField topicField;

    @FXML
    public Button loginButton;

    private String username;
    private String topic;

    @FXML
    private void initialize() {
        errorLabel.setText("");
        loginButton.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(topicField.textProperty().isEmpty())
        );

        loginButton.setOnAction(event -> handleLogin());
    }

    private Runnable onLoginSuccess;

    public void handleLogin() {
        String username = usernameField.getText();
        String topic = topicField.getText();
        if (username.isBlank() || topic.isBlank()) {
            errorLabel.setText("Username and Topic cannot be empty.");
        } else {
            errorLabel.setText("Logged in as " + username + " on topic " + topic);
            this.topic = topic;
            this.username = username;
        }
        if (onLoginSuccess != null) {
            onLoginSuccess.run();
        }

    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public String getUsername() {
        return username;
    }

    public String getTopic() {
        return topic;
    }
}

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

    private final ChatModel model = new ChatModel();
    public Button loginButton;

    @FXML
    private void initialize() {
        errorLabel.setText("");
        loginButton.disableProperty().bind(
                usernameField.textProperty().isEmpty()
//                        .or(passwordField.textProperty().isEmpty())
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
            model.setTopic(topic);
            model.setUsername(username);
        }
        if (onLoginSuccess != null) {
            onLoginSuccess.run();
        }

    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }
}

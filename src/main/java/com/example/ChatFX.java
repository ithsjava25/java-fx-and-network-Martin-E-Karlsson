package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loginLoader = new FXMLLoader(ChatFX.class.getResource("login-view.fxml"));
        Parent loginRoot = loginLoader.load();

        Scene loginScene = new Scene(loginRoot, 300, 140);
        stage.setTitle("Login");
        stage.setScene(loginScene);
        stage.show();

        LoginController loginController = loginLoader.getController();

        loginController.setOnLoginSuccess(() -> {
            String username = loginController.getUsername();
            String topic = loginController.getTopic();
            ChatModel model = new ChatModel(username, topic);
            try {
                FXMLLoader chatLoader = new FXMLLoader(ChatFX.class.getResource("chat-view.fxml"));
                Parent chatRoot = chatLoader.load();

                ChatController chatController = chatLoader.getController();
                chatController.setModel(model);

                Scene chatScene = new Scene(chatRoot, 640, 480);
                stage.setTitle("MEK Chat");
                stage.setScene(chatScene);
                stage.show();
            } catch (IOException e) {
                System.err.println("Failed to load chat view: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
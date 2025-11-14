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

        Scene loginScene = new Scene(loginRoot, 400, 300);
        stage.setTitle("Login");
        stage.setScene(loginScene);
        stage.show();

        LoginController loginController = loginLoader.getController();

        loginController.setOnLoginSuccess(() -> {
            try {
                FXMLLoader chatLoader = new FXMLLoader(ChatFX.class.getResource("chat-view.fxml"));
                Parent chatRoot = chatLoader.load();

                Scene chatScene = new Scene(chatRoot, 640, 480);
                stage.setTitle("MEK Chat");
                stage.setScene(chatScene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



//        FXMLLoader chatLoader = new FXMLLoader(ChatFX.class.getResource("chat-view.fxml"));
//        Parent root = chatLoader.load();
//        Scene scene = new Scene(root, 640, 480);
//        stage.setTitle("MEK Chat");
//        stage.setScene(scene);
//        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
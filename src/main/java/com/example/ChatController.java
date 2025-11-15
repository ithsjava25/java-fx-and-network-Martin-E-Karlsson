package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

/**
 * Controller layer: mediates between the view (FXML) and the model.
 */
public class ChatController {

    private ChatModel model;
    public ListView<NtfyMessageDto> messageView;

    @FXML
    public TextField inputField;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
    }

    public void setModel(ChatModel model) {
        messageLabel.setText(model.getTopic());
        messageView.setItems(model.getMessages());
        this.model = model;
        if (messageLabel != null) {
            messageLabel.setText(model.getTopic() + " — " + model.getUsername());
        }

        if (messageView != null) {
            messageView.setItems(model.getMessages());

            // custom cell factory: username + bubble + alignment
            messageView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(NtfyMessageDto item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    String sender = extractSender(item);
                    String body = extractBody(item);

                    Label userLabel = new Label(sender);
                    userLabel.getStyleClass().add("message-user");
                    userLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                    Label msgLabel = new Label(body);
                    msgLabel.setWrapText(true);
                    msgLabel.setMaxWidth(360);
                    msgLabel.getStyleClass().add("message-bubble");
                    msgLabel.setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-background-color: lightgray;");

                    VBox bubbleVBox = new VBox(2, userLabel, msgLabel);

                    Region spacer = new Region();
                    HBox hbox = new HBox(8);
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    boolean isMe = model.getUsername() != null && model.getUsername().equals(sender);

                    if (isMe) {
                        // user messages: align right and change bubble color
                        msgLabel.setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-background-color: -fx-accent;" +
                                " -fx-text-fill: white;");
                        hbox.getChildren().addAll(spacer, bubbleVBox);
                        hbox.setAlignment(Pos.CENTER_RIGHT);
                    } else {
                        // other messages: align left
                        hbox.getChildren().addAll(bubbleVBox, spacer);
                        hbox.setAlignment(Pos.CENTER_LEFT);
                    }

                    setText(null);
                    setGraphic(hbox);
                }
            });
        }
    }

    public void sendMessage(ActionEvent actionEvent) {
        if (model == null) return;
        model.setMessageToSend(inputField.getText());
        inputField.clear();
        model.sendMessage();
    }

    private String extractSender(NtfyMessageDto item) {
        // TODO: include user in DTO information
        if (item == null) return "unknown";
        // try common getter names; adapt to your DTO
        try {
            if (item.getUser() != null) return item.getUser();
        } catch (Exception ignored) {}
        return item.toString();
    }

    private String extractBody(NtfyMessageDto item) {
        if (item == null) return "";
        try {
            if (item.getMessage() != null) return item.getMessage();
        } catch (Exception ignored) {}
        return item.toString();
    }
}
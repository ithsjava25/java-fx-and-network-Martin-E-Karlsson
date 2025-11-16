package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Controller layer: mediates between the view (FXML) and the model.
 */
public class ChatController {

    private ChatModel model;

    @FXML
    public Button sendButton;

    @FXML
    public Button sendImageButton;

    @FXML
    public ListView<NtfyMessageDto> messageView;

    @FXML
    public TextField inputField;

    @FXML
    private Label messageLabel;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded. If images for buttons are not found, fallback to text.
     */
    @FXML
    private void initialize() {
        try {
            Image sendIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/send.png")));
            ImageView imageView = new ImageView(sendIcon);
            imageView.setFitHeight(30);
            imageView.setFitWidth(30);
            imageView.setPreserveRatio(true);

            sendButton.setGraphic(imageView);
            sendButton.setText("");
        } catch (NullPointerException e) {
            sendButton.setText("Send Message");
        }

        try {
            Image attachIcon = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/com/example/image.png")));
            ImageView imageView = new ImageView(attachIcon);
            imageView.setFitHeight(30);
            imageView.setFitWidth(30);
            imageView.setPreserveRatio(true);

            sendImageButton.setGraphic(imageView);
            sendImageButton.setText("");
        } catch (NullPointerException e) {
            sendImageButton.setText("Send Image");
        }

        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                doSendMessage();
            }
        });
    }

    /**
     * Binds the model to the controller and sets up data bindings.
     * Also, dynamically updates the message view cell factory to format messages.
     * @param model The ChatModel instance to bind.
     */
    public void setModel(ChatModel model) {
        messageView.setItems(model.getMessages());
        this.model = model;
        messageLabel.setText(model.getTopic() + " — " + model.getUsername());

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

                Label userLabel = new Label(sender + " " + LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(item.time()),
                        ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)
                );
                userLabel.getStyleClass().add("message-user");
                userLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                Label msgLabel = new Label(body);
                msgLabel.setWrapText(true);
                msgLabel.setMaxWidth(360);
                msgLabel.getStyleClass().add("message-bubble");
                msgLabel.setStyle("-fx-padding: 4; -fx-background-radius: 8; -fx-background-color: lightgray;");

                VBox bubbleVBox = new VBox(2, userLabel, msgLabel);

                Region spacer = new Region();
                HBox hbox = new HBox(4);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                boolean isMe = model.getUsername() != null && model.getUsername().equals(sender);

                if (isMe) {
                    msgLabel.setStyle("-fx-padding: 4; -fx-background-radius: 8; -fx-background-color: -fx-accent;" +
                            " -fx-text-fill: white;");
                    hbox.getChildren().addAll(spacer, bubbleVBox);
                    hbox.setAlignment(Pos.CENTER_RIGHT);
                } else {
                    hbox.getChildren().addAll(bubbleVBox, spacer);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                }

                setText(null);
                setGraphic(hbox);
            }
        });
    }

    /**
     * Handles the send message button action.
     */
    public void sendMessage(ActionEvent actionEvent) {
        doSendMessage();
    }

    /**
     * Takes a call either from a send button activation or enter key press then sends the content
     * of the input field to ChatModel.
     */
    public void doSendMessage(){
        if (model == null) return;
        model.setMessageToSend(inputField.getText());
        inputField.clear();
        model.sendMessage();
    }

    /**
     * Extracts the username from the message item.
     * @param item The message item.
     * @return The extracted sender name or "unknown".
     */
    private String extractSender(NtfyMessageDto item) {
        String user = item.getUser();
        return user != null ? user : "unknown";
    }

    /**
     * Extracts the message body from the message item.
     * @param item The message item.
     * @return The extracted message body or the item's toString().
     */
    private String extractBody(NtfyMessageDto item) {
        String message = item.getMessage();
        return message != null ? message : "";
    }

    /**
     * Handles the send image button action.
     * Opens a file chooser to select an image and sends it via the model.
     * Not fully implemented feature.
     */
    public void sendImage(ActionEvent actionEvent) {
        if (model == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image to Send");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(sendImageButton.getScene().getWindow());
        if (selectedFile != null) {
            String imagePath = selectedFile.toURI().toString();
            model.setImageToSend(imagePath);
            model.sendImage();
        }

    }
}
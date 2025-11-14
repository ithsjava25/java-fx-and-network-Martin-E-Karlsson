package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller layer: mediates between the view (FXML) and the model.
 */
public class ChatController {

    private final ChatModel model = new ChatModel();
    public ListView<NtfyMessageDto> messageView;

    @FXML
    public TextField inputField;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        // ToDO : View as message string not NtfyMessageDto object
        messageLabel.setText(model.getTopic());
        messageView.setItems(model.getMessages());
    }

    public void sendMessage(ActionEvent actionEvent) {
        model.setMessageToSend(inputField.getText());
        inputField.clear();
        model.sendMessage();
    }
}
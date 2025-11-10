package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Controller layer: mediates between the view (FXML) and the model.
 */
public class ChatController {

    private final ChatModel model = new ChatModel(new NtfyConnectionImpl());
    public ListView<NtfyMessageDto> messageView;
    public TextField inputField;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
//        if (messageLabel != null) {
//            messageLabel.setText(model.getGreeting());
//        }
        // ToDO : View as message string not NtfyMessageDto object
        messageView.setItems(model.getMessages());
    }

    public void sendMessage(ActionEvent actionEvent) {
        model.setMessageToSend(inputField.getText());
        inputField.clear();
        model.sendMessage();
    }
}
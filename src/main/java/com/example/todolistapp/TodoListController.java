package com.example.todolistapp;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import java.util.prefs.Preferences;

public class TodoListController {

    @FXML private Button minButton;
    @FXML private Button maxButton;
    @FXML private Button closeButton;

    private double xOffset, yOffset;

    @FXML
    private void onTitleBarPressed(MouseEvent e) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        xOffset = e.getScreenX() - stage.getX();
        yOffset = e.getScreenY() - stage.getY();
    }

    @FXML
    private void onTitleBarDragged(MouseEvent e) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setX(e.getScreenX() - xOffset);
        stage.setY(e.getScreenY() - yOffset);
    }

    @FXML
    private void onMinimize() {
        ((Stage) minButton.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void onMaximize() {
        Stage stage = (Stage) maxButton.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void onClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    //Title field (on enter keep that title)
    @FXML private TextField titleField;

    private final Preferences prefs = Preferences.userNodeForPackage(TodoListController.class);

    @FXML
    private void initialize() {
        titleField.setText(prefs.get("listName", ""));
    }

    @FXML
    private void onTitleEntered() {
        String text = titleField.getText().trim();
        if (text.isEmpty()) return;

        prefs.put("listName", text);
        titleField.getParent().requestFocus(); // drops the cursor out of the field
    }
}
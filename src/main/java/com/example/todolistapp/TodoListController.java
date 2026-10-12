package com.example.todolistapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.TextField;
import java.io.IOException;
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

    //Add task button (+): opens the "New task" window
    @FXML
    private void onAddTask() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("add-task-view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.setFill(Color.TRANSPARENT);

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(titleField.getScene().getWindow());
        dialog.setScene(scene);
        dialog.showAndWait();

        AddTaskController.TaskData task = loader.<AddTaskController>getController().getResult();
        if (task != null) {
            // task.name() and task.repeat() (NONE, DAILY, WEEKLY, MONTHLY) are ready to use here
        }
    }
}

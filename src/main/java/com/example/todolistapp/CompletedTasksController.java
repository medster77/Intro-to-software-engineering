package com.example.todolistapp;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;

public class CompletedTasksController {

    @FXML private HBox dragBar;
    @FXML private Label countLabel;
    @FXML private Label emptyLabel;
    @FXML private VBox completedList;

    private double xOffset;
    private double yOffset;

    /** Fills the window with the given task names. Call right after loading the FXML. */
    public void setTasks(List<String> names) {
        completedList.getChildren().clear();

        boolean empty = names.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        countLabel.setText(names.size() == 1 ? "1 task completed" : names.size() + " tasks completed");

        for (String name : names) {
            Label tick = new Label("✓");
            tick.setStyle("-fx-text-fill: #10A37F; -fx-font-size: 15px; -fx-font-weight: bold;");

            Text taskText = new Text(name);
            taskText.setStrikethrough(true);
            taskText.setWrappingWidth(320);
            taskText.setStyle("-fx-fill: #8E8EA0; -fx-font-size: 14px;");

            HBox row = new HBox(12, tick, taskText);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.setStyle("-fx-background-color: #2A2A2A; -fx-background-radius: 8;");
            completedList.getChildren().add(row);
        }
    }

    @FXML
    private void onClose() {
        ((Stage) dragBar.getScene().getWindow()).close();
    }

    @FXML
    private void onDragPressed(MouseEvent e) {
        Stage stage = (Stage) dragBar.getScene().getWindow();
        xOffset = e.getScreenX() - stage.getX();
        yOffset = e.getScreenY() - stage.getY();
    }

    @FXML
    private void onDragged(MouseEvent e) {
        Stage stage = (Stage) dragBar.getScene().getWindow();
        stage.setX(e.getScreenX() - xOffset);
        stage.setY(e.getScreenY() - yOffset);
    }
    @FXML
    private void onClearHistory() {
        try {
            TodoListController.clearCompletedHistory();

            completedList.getChildren().clear();

            countLabel.setText("0 tasks completed");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }
}

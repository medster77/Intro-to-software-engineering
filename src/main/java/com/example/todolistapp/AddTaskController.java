package com.example.todolistapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class AddTaskController {

    public enum Repeat { NONE, DAILY, WEEKLY, MONTHLY }

    /** What the dialog hands back to the main window. */
    public record TaskData(String name, Repeat repeat) {}

    private static final String TOGGLE_BASE =
            "-fx-background-insets: 0; -fx-background-radius: 8; -fx-font-size: 13px; "
                    + "-fx-cursor: hand; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; ";
    private static final String TOGGLE_ON  = TOGGLE_BASE + "-fx-background-color: #10A37F; -fx-text-fill: #FFFFFF;";
    private static final String TOGGLE_OFF = TOGGLE_BASE + "-fx-background-color: #2F2F2F; -fx-text-fill: #B4B4B4;";

    @FXML private HBox dragBar;
    @FXML private TextField taskNameField;
    @FXML private Label errorLabel;
    @FXML private ToggleGroup repeatGroup;

    private String fieldStyle;
    private double xOffset;
    private double yOffset;
    private TaskData result;   // stays null if the user cancels

    @FXML
    private void initialize() {
        fieldStyle = taskNameField.getStyle();

        // Clear the error as soon as the user types
        taskNameField.textProperty().addListener((obs, oldText, newText) -> clearError());

        // Highlight the selected repeat option, and don't allow deselecting everything
        repeatGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                repeatGroup.selectToggle(oldToggle);
                return;
            }
            restyleToggles();
        });
        restyleToggles();

        Platform.runLater(taskNameField::requestFocus);
    }

    private void restyleToggles() {
        for (Toggle t : repeatGroup.getToggles()) {
            ((ToggleButton) t).setStyle(t.isSelected() ? TOGGLE_ON : TOGGLE_OFF);
        }
    }

    @FXML
    private void onSave() {
        String name = taskNameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            taskNameField.setStyle(fieldStyle + "-fx-border-color: #F07178; -fx-border-radius: 8;");
            taskNameField.requestFocus();
            return;
        }
        Toggle selected = repeatGroup.getSelectedToggle();
        Repeat repeat = Repeat.valueOf((String) selected.getUserData());
        result = new TaskData(name, repeat);
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
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

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        taskNameField.setStyle(fieldStyle);
    }

    private void closeWindow() {
        ((Stage) dragBar.getScene().getWindow()).close();
    }

    /** Returns the entered task, or null if the dialog was cancelled. */
    public TaskData getResult() {
        return result;
    }
}

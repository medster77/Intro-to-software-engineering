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
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            //find the first empty line
            int slotIndex = -1;

            for (int i = 0; i < taskList.getChildren().size(); i++) {
                HBox row = (HBox) taskList.getChildren().get(i);

                if (row.getChildren().isEmpty()) {
                    slotIndex = i;
                    break;
                }
            }

            if (slotIndex == -1) {
                System.out.println("No empty notebook lines left!");
                return;
            }

            // create a checkbox for the task
            CheckBox taskCheckBox = new CheckBox();
            Text taskText = new Text(task.name());

            // Style the task text
            taskText.setStyle("-fx-fill: #ECECEC;" + "-fx-font-size: 14px;");

            taskCheckBox.setGraphic(taskText);
            taskCheckBox.setStyle("-fx-cursor: hand;");

            // Cross out the text when checked
            taskCheckBox.setOnAction(e -> {
                taskText.setStrikethrough(taskCheckBox.isSelected());

                // Move completed tasks to the bottom
                reorderTaskRows();
            });
            //adds task to the next line
            HBox taskRow = (HBox) taskList.getChildren().get(slotIndex);

            taskRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            taskRow.setPadding(new javafx.geometry.Insets(0, 10, 0, 10));
            taskRow.getChildren().add(taskCheckBox);


        }
    }
    //Completed tasks button (✓): opens a window listing every checked-off task
    @FXML
    private void onShowCompleted() throws IOException {
        List<String> completed = new ArrayList<>();

        for (Node node : taskList.getChildren()) {
            HBox row = (HBox) node;

            for (Node child : row.getChildren()) {
                if (child instanceof CheckBox checkBox
                        && checkBox.isSelected()
                        && checkBox.getGraphic() instanceof Text taskText) {
                    completed.add(taskText.getText());
                }
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("completed-tasks-view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.setFill(Color.TRANSPARENT);
        loader.<CompletedTasksController>getController().setTasks(completed);

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(titleField.getScene().getWindow());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private VBox taskList;
    private void reorderTaskRows() {
        ArrayList<CheckBox> tasks = new ArrayList<>();

        // get all tasks
        for (javafx.scene.Node node : taskList.getChildren()) {
            HBox row = (HBox) node;

            for (javafx.scene.Node child : row.getChildren()) {
                if (child instanceof CheckBox checkBox) {
                    tasks.add(checkBox);
                }
            }
        }

        // Unchecked tasks first, completed tasks last
        tasks.sort(Comparator.comparing(CheckBox::isSelected));

        // Clear rows without deleting lines
        for (javafx.scene.Node node : taskList.getChildren()) {
            ((HBox) node).getChildren().clear();
        }

        // Put the tasks back in the new order
        for (int i = 0; i < tasks.size(); i++) {
            HBox row = (HBox) taskList.getChildren().get(i);

            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setPadding(new javafx.geometry.Insets(0, 10, 0, 10));
            row.getChildren().add(tasks.get(i));
        }
    }
}
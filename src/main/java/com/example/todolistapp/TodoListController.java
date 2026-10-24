package com.example.todolistapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.time.LocalDate;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

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

    private Timeline repeatChecker;

    /** Extra info kept on every task's checkbox (via setUserData). */
    private static class TaskMeta {
        final AddTaskController.Repeat repeat;
        LocalDate completedOn;   // null while the task is unchecked

        TaskMeta(AddTaskController.Repeat repeat, LocalDate completedOn) {
            this.repeat = repeat;
            this.completedOn = completedOn;
        }
    }

    @FXML
    private void initialize() {
        titleField.setText(prefs.get("listName", ""));
        loadTasks();
        refreshRepeatingTasks();   // bring back anything that is due again

        // Re-check every minute so tasks also reappear if the app is left open past midnight
        repeatChecker = new Timeline(new KeyFrame(Duration.minutes(1), e -> refreshRepeatingTasks()));
        repeatChecker.setCycleCount(Animation.INDEFINITE);
        repeatChecker.play();
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
        if (task != null && addTaskToList(task.name(), task.repeat(), false, null)) {
            saveTasks();
        }
    }

    /** Puts a task into the first empty notebook line. Returns false if every line is taken. */
    private boolean addTaskToList(String name, AddTaskController.Repeat repeat, boolean done, LocalDate completedOn) {
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
            return false;
        }

        // create a checkbox for the task
        CheckBox taskCheckBox = new CheckBox();
        Text taskText = new Text(name);

        // Create a delete button for this task
        Button deleteButton = new Button("×");
        deleteButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #ff6b6b;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;"
        );
        deleteButton.setFocusTraversable(false);
        deleteButton.setUserData(taskCheckBox);

        deleteButton.setOnAction(e -> {
            // Remove this task from its notebook line
            for (Node node : taskList.getChildren()) {
                HBox row = (HBox) node;

                if (row.getChildren().contains(taskCheckBox)) {
                    row.getChildren().clear();
                    break;
                }
            }

            // Rearrange remaining tasks and save the changes
            reorderTaskRows();
            saveTasks();
        });

        // Style the task text
        taskText.setStyle("-fx-fill: #ECECEC;" + "-fx-font-size: 14px;");
        taskText.setStrikethrough(done);

        taskCheckBox.setSelected(done);
        // a task that is already done but has no date yet counts as finished today
        TaskMeta meta = new TaskMeta(repeat, done ? (completedOn != null ? completedOn : LocalDate.now()) : null);
        taskCheckBox.setUserData(meta);     // remembered so it can be saved and rescheduled
        taskCheckBox.setGraphic(taskText);
        taskCheckBox.setStyle("-fx-cursor: hand;");

        // Cross out the text when checked and uncrosses when unchecked

        taskCheckBox.setOnAction(e -> {
            taskText.setStrikethrough(taskCheckBox.isSelected());

            meta.completedOn = taskCheckBox.isSelected()
                    ? LocalDate.now()
                    : null;

            // Archive completed one-time tasks permanently.
            if (taskCheckBox.isSelected()
                    && meta.repeat == AddTaskController.Repeat.NONE) {
                archiveCompletedTask(taskText.getText());
            }

            // When a repeating task is unchecked, remove its archived entry.
            if (!taskCheckBox.isSelected()) {
                removeCompletedTask(taskText.getText());
            }

            reorderTaskRows();
            saveTasks();
        });

        //adds task to the next line
        HBox taskRow = (HBox) taskList.getChildren().get(slotIndex);

        taskRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        taskRow.setPadding(new javafx.geometry.Insets(0, 10, 0, 10));
        taskRow.setSpacing(8);

        taskCheckBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(taskCheckBox, Priority.ALWAYS);

        taskRow.getChildren().addAll(taskCheckBox, deleteButton);
        return true;
    }

    //Saving: one task per line ->  done<TAB>repeat<TAB>completedOn<TAB>name
    // Lives in %APPDATA%\TodoListApp\tasks.txt so it still works once packaged as an .exe
    private static Path saveFile() {
        String appData = System.getenv("APPDATA");
        Path base = (appData != null)
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"), ".config");

        return base.resolve("TodoListApp").resolve("tasks.txt");
    }

    private static Path completedSaveFile() {
        String appData = System.getenv("APPDATA");
        Path base = (appData != null)
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"), ".config");

        return base.resolve("TodoListApp").resolve("completed.txt");
    }

    private List<String> loadCompletedTasks() {
        Path file = completedSaveFile();

        try {
            if (Files.exists(file)) {
                return new ArrayList<>(
                        Files.readAllLines(file, StandardCharsets.UTF_8)
                );
            }
        } catch (IOException ex) {
            System.err.println("Could not load completed tasks: " + ex.getMessage());
        }

        return new ArrayList<>();
    }

    private void archiveCompletedTask(String name) {
        List<String> completed = loadCompletedTasks();

        // Avoid archiving the same task name twice.
        if (completed.contains(name)) {
            return;
        }

        completed.add(name);

        try {
            Path file = completedSaveFile();
            Files.createDirectories(file.getParent());
            Files.write(file, completed, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Could not archive completed task: " + ex.getMessage());
        }
    }

    private void saveTasks() {
        List<String> lines = new ArrayList<>();

        for (Node node : taskList.getChildren()) {
            HBox row = (HBox) node;

            for (Node child : row.getChildren()) {
                if (child instanceof CheckBox checkBox && checkBox.getGraphic() instanceof Text taskText) {
                    TaskMeta meta = (TaskMeta) checkBox.getUserData();
                    String completedOn = (meta.completedOn != null) ? meta.completedOn.toString() : "";
                    String name = taskText.getText().replace('\t', ' ').replace('\n', ' ');
                    lines.add((checkBox.isSelected() ? "1" : "0") + "\t" + meta.repeat
                            + "\t" + completedOn + "\t" + name);
                }
            }
        }

        try {
            Path file = saveFile();
            Files.createDirectories(file.getParent());

            // write to a temp file first so a crash mid-save can't corrupt the real one
            Path temp = file.resolveSibling("tasks.txt.tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            System.err.println("Could not save tasks: " + ex.getMessage());
        }
    }


    private void loadTasks() {
        Path file = saveFile();
        if (!Files.exists(file)) return;

        boolean removedCompletedTasks = false;

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", 4);
                if (parts.length < 3) continue;

                String name = (parts.length == 4) ? parts[3] : parts[2];
                String dateText = (parts.length == 4) ? parts[2] : "";

                if (name.isBlank()) continue;

                LocalDate completedOn = null;
                try {
                    if (!dateText.isBlank()) {
                        completedOn = LocalDate.parse(dateText);
                    }
                } catch (java.time.format.DateTimeParseException ex) {
                    completedOn = null;
                }

                AddTaskController.Repeat repeat;
                try {
                    repeat = AddTaskController.Repeat.valueOf(parts[1]);
                } catch (IllegalArgumentException ex) {
                    repeat = AddTaskController.Repeat.NONE;
                }

                boolean done = parts[0].equals("1");

                // Remove completed, non repeat tasks from the active list and archive to completed file
                if (done && repeat == AddTaskController.Repeat.NONE) {
                    archiveCompletedTask(name);
                    removedCompletedTasks = true;
                    continue;
                }

                addTaskToList(name, repeat, done, completedOn);
            }

            // Persist the cleaned active list.
            if (removedCompletedTasks) {
                saveTasks();
            }

        } catch (IOException ex) {
            System.err.println("Could not load tasks: " + ex.getMessage());
        }
    }

    //Scheduling: a checked repeating task comes back (unchecked) once its period has passed.
    //  Daily   -> the day after it was completed
    //  Weekly  -> 7 days after
    //  Monthly -> same date next month
    private static boolean isDue(TaskMeta meta) {
        if (meta.completedOn == null) return false;

        LocalDate comesBack = switch (meta.repeat) {
            case DAILY   -> meta.completedOn.plusDays(1);
            case WEEKLY  -> meta.completedOn.plusWeeks(1);
            case MONTHLY -> meta.completedOn.plusMonths(1);
            case NONE    -> null;
        };
        return comesBack != null && !LocalDate.now().isBefore(comesBack);
    }

    private void refreshRepeatingTasks() {
        boolean changed = false;

        for (Node node : taskList.getChildren()) {
            HBox row = (HBox) node;

            for (Node child : row.getChildren()) {
                if (child instanceof CheckBox checkBox
                        && checkBox.isSelected()
                        && checkBox.getGraphic() instanceof Text taskText
                        && checkBox.getUserData() instanceof TaskMeta meta
                        && isDue(meta)) {
                    checkBox.setSelected(false);
                    taskText.setStrikethrough(false);
                    meta.completedOn = null;
                    changed = true;
                }
            }
        }

        if (changed) {
            reorderTaskRows();   // moves the revived tasks back up with the unchecked ones
            saveTasks();
        }
    }

    //Completed tasks button (✓): opens a window listing every checked-off task
    @FXML
    private void onShowCompleted() throws IOException {
        // Start with tasks saved in the permanent archive.
        List<String> completed = loadCompletedTasks();

        // Also show checked repeating tasks that are still in the notebook.
        for (Node node : taskList.getChildren()) {
            HBox row = (HBox) node;

            for (Node child : row.getChildren()) {
                if (child instanceof CheckBox checkBox
                        && checkBox.isSelected()
                        && checkBox.getGraphic() instanceof Text taskText
                        && checkBox.getUserData() instanceof TaskMeta meta
                        && meta.repeat != AddTaskController.Repeat.NONE) {
                    completed.add(taskText.getText());
                }
            }
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("completed-tasks-view.fxml")
        );
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
        java.util.Map<CheckBox, Button> deleteButtons =
                new java.util.HashMap<>();

        // Collect each checkbox and its matching delete button
        for (Node node : taskList.getChildren()) {
            HBox row = (HBox) node;

            for (Node child : row.getChildren()) {
                if (child instanceof CheckBox checkBox) {
                    tasks.add(checkBox);
                } else if (child instanceof Button button
                        && button.getUserData() instanceof CheckBox checkBox) {
                    deleteButtons.put(checkBox, button);
                }
            }
        }

        // Unchecked tasks first, completed tasks last
        tasks.sort(Comparator.comparing(CheckBox::isSelected));

        // Clear the notebook lines
        for (Node node : taskList.getChildren()) {
            ((HBox) node).getChildren().clear();
        }

        // Put each task and its delete button back together
        for (int i = 0; i < tasks.size(); i++) {
            HBox row = (HBox) taskList.getChildren().get(i);
            CheckBox checkBox = tasks.get(i);
            Button deleteButton = deleteButtons.get(checkBox);

            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setPadding(new javafx.geometry.Insets(0, 10, 0, 10));
            row.setSpacing(8);

            checkBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(checkBox, Priority.ALWAYS);

            row.getChildren().add(checkBox);

            if (deleteButton != null) {
                row.getChildren().add(deleteButton);
            }
        }
    }
    //clear completed history
    public static void clearCompletedHistory() throws IOException {
        Path file = completedSaveFile();

        Files.createDirectories(file.getParent());
        Files.write(
                file,
                new byte[0]
        );
    }

    // remove unchecked task from completed folder
    private void removeCompletedTask(String taskName) {
        Path file = completedSaveFile();

        try {
            if (Files.exists(file)) {
                List<String> completed = new ArrayList<>(
                        Files.readAllLines(file, StandardCharsets.UTF_8)
                );

                completed.remove(taskName);

                Files.write(
                        file,
                        completed,
                        StandardCharsets.UTF_8
                );
            }
        } catch (IOException ex) {
            System.err.println(
                    "Could not remove completed task: " + ex.getMessage()
            );
        }
    }
}
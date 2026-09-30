package com.example.todolistapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class TodoListApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TodoListApplication.class.getResource("todo-list-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());

        // 2. Make the scene background transparent to hide the white corners
        scene.setFill(Color.TRANSPARENT);

        stage.setTitle("Hello!");

        // 3. Make the operating system window border borderless and transparent
        stage.initStyle(StageStyle.TRANSPARENT);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

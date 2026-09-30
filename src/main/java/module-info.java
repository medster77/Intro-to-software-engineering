module com.example.todolistapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.todolistapp to javafx.fxml;
    exports com.example.todolistapp;
}
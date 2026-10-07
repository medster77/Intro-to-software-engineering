module com.example.todolistapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;

    opens com.example.todolistapp to javafx.fxml;
    exports com.example.todolistapp;
}
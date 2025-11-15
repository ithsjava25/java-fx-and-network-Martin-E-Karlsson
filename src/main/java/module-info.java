module chatfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.github.cdimascio.dotenv.java;
    requires java.net.http;
    requires javafx.graphics;
    requires javafx.base;
    requires com.fasterxml.jackson.databind;

    opens com.example to javafx.fxml;
    exports com.example;
}
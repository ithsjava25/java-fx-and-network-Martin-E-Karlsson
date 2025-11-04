module hellofx {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.github.cdimascio.dotenv.java;
    requires java.net.http;
    requires tools.jackson.databind;
    requires javafx.graphics;

    opens com.example to javafx.fxml;
    exports com.example;
}
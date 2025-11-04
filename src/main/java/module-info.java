module hellofx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires io.github.cdimascio.dotenv.java;
    requires tools.jackson.databind;

    opens com.example to javafx.fxml;
    exports com.example;
}
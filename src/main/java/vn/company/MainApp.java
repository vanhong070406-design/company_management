package vn.company;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vn/company/view/main.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 760);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/vn/company/view/style.css")).toExternalForm());

        stage.setTitle("COMPANY - Quản lý nhân sự");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

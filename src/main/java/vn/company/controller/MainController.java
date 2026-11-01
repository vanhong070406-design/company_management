package vn.company.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import vn.company.util.DBConnection;
import vn.company.util.Dialogs;

import java.sql.Connection;
import java.sql.SQLException;

/** Cửa sổ chính: kiểm tra kết nối rồi tải dữ liệu nhân viên. */
public class MainController {

    @FXML private Label lblConnection;

    // fx:include fx:id="employeeView" -> FXMLLoader tự gán controller vào biến employeeViewController
    @FXML private EmployeeController employeeViewController;

    @FXML
    private void initialize() {
        // Đợi cửa sổ hiện lên rồi mới kết nối, để hộp thoại lỗi (nếu có) nằm trên cửa sổ chính
        Platform.runLater(this::connect);
    }

    @FXML
    private void onReconnect() {
        connect();
    }

    private void connect() {
        try (Connection conn = DBConnection.getConnection()) {
            lblConnection.setText("● Đã kết nối " + conn.getCatalog() + " (" + DBConnection.getUser() + ")");
            lblConnection.getStyleClass().remove("disconnected");
            employeeViewController.refresh();
        } catch (SQLException e) {
            lblConnection.setText("● Chưa kết nối được database");
            if (!lblConnection.getStyleClass().contains("disconnected")) {
                lblConnection.getStyleClass().add("disconnected");
            }
            Dialogs.connectionError(e);
        }
    }
}

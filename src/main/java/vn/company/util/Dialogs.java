package vn.company.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.sql.SQLException;
import java.util.List;

/** Các hộp thoại dùng chung. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle("Thông báo");
        a.setHeaderText(null);
        fit(a);
        a.showAndWait();
    }

    public static void warn(String header, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.setTitle("Cảnh báo");
        a.setHeaderText(header);
        fit(a);
        a.showAndWait();
    }

    public static void validation(List<String> errors) {
        warn("Dữ liệu chưa hợp lệ", "• " + String.join("\n• ", errors));
    }

    public static boolean confirm(String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle("Xác nhận");
        a.setHeaderText(header);
        fit(a);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /** Hiển thị lỗi SQL kèm giải thích tiếng Việt cho các mã lỗi SQL Server hay gặp. */
    public static void sqlError(String header, SQLException e) {
        e.printStackTrace();
        String hint = switch (e.getErrorCode()) {
            case 2627, 2601 -> "Khóa chính bị trùng: bản ghi này đã tồn tại.";
            case 547 -> "Vi phạm ràng buộc khóa ngoại: dữ liệu đang được bảng khác tham chiếu, "
                    + "hoặc tham chiếu tới bản ghi không tồn tại.";
            case 8152, 2628 -> "Dữ liệu quá dài so với độ dài cột trong database.";
            case 8115 -> "Giá trị số vượt quá giới hạn của cột.";
            case 18456 -> "Đăng nhập SQL Server thất bại: kiểm tra user/password trong db.properties.";
            case 4060 -> "Không mở được database CompanyDB: hãy chạy sql/01_schema.sql trước.";
            default -> null;
        };
        String msg = (hint == null ? "" : hint + "\n\n") + "Chi tiết: " + e.getMessage();
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Lỗi cơ sở dữ liệu");
        a.setHeaderText(header);
        a.getDialogPane().setPrefWidth(520);
        fit(a);
        a.showAndWait();
    }

    public static void connectionError(SQLException e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR,
                e.getMessage() + "\n\nKiểm tra:\n"
                        + "• Dịch vụ SQL Server đang chạy, đã bật TCP/IP ở cổng 1433\n"
                        + "• Đã bật SQL Server Authentication và user sa không bị Disabled\n"
                        + "• Đã chạy sql/01_schema.sql để tạo database CompanyDB\n"
                        + "• URL, user, password trong src/main/resources/db.properties",
                ButtonType.OK);
        a.setTitle("Lỗi kết nối");
        a.setHeaderText("Không kết nối được SQL Server");
        a.getDialogPane().setPrefWidth(560);
        fit(a);
        a.showAndWait();
    }

    /** Cho hộp thoại tự giãn chiều cao theo nội dung (mặc định JavaFX cắt chữ dài). */
    private static void fit(Alert a) {
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.setResizable(true);
    }
}

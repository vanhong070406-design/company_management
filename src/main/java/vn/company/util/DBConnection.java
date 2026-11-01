package vn.company.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Tạo kết nối tới SQL Server theo cấu hình trong db.properties. */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("Không tìm thấy file db.properties");
            }
            PROPS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            DriverManager.setLoginTimeout(5); // tránh treo giao diện lâu khi cấu hình sai
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
    }

    /** Mỗi lần gọi trả về một Connection mới; luôn đóng bằng try-with-resources. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
    }

    public static String getUser() {
        return PROPS.getProperty("db.user");
    }
}

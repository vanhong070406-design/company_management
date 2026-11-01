package vn.company.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/** Các hàm nhỏ dùng chung để gán tham số có thể NULL và đọc ngày tháng. */
final class Jdbc {

    private Jdbc() {
    }

    static void setString(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(i, Types.VARCHAR);
        else ps.setString(i, v.trim());
    }

    static void setDate(PreparedStatement ps, int i, LocalDate v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DATE);
        else ps.setDate(i, Date.valueOf(v));
    }

    static void setDecimal(PreparedStatement ps, int i, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DECIMAL);
        else ps.setBigDecimal(i, v);
    }

    static LocalDate getDate(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    /** CHAR(n) trong SQL Server được đệm khoảng trắng, nên cắt bỏ khi đọc ra. */
    static String getTrimmed(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? null : s.trim();
    }

    /** Pattern cho LIKE '%keyword%' */
    static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim()) + "%";
    }
}

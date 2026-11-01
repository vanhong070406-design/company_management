package vn.company.dao;

import vn.company.model.Department;
import vn.company.util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    /** Danh sách gọn (mã + tên) cho ComboBox phòng ban ở form nhân viên. */
    public List<Department> findLookup() throws SQLException {
        List<Department> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT Dnumber, Dname FROM DEPARTMENT ORDER BY Dnumber")) {
            while (rs.next()) {
                list.add(new Department(rs.getInt("Dnumber"), rs.getString("Dname")));
            }
        }
        return list;
    }
}

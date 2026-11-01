package vn.company.dao;

import vn.company.model.Employee;
import vn.company.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    /** Số bản ghi đang tham chiếu tới một nhân viên (để cảnh báo trước khi xóa). */
    public record References(int worksOn, int dependents, int subordinates, int managedDepartments) {
        public boolean isEmpty() {
            return worksOn + dependents + subordinates + managedDepartments == 0;
        }
    }

    // ------------------------------------------------------------------ READ

    public List<Employee> search(String keyword) throws SQLException {
        String sql = """
                SELECT e.Ssn, e.Fname, e.Minit, e.Lname, e.Bdate, e.Address, e.Sex, e.Salary,
                       e.Super_ssn, e.Dno,
                       CASE WHEN s.Ssn IS NULL THEN NULL ELSE CONCAT(s.Fname, ' ', s.Lname) END AS SuperName,
                       d.Dname
                FROM EMPLOYEE e
                LEFT JOIN EMPLOYEE s   ON s.Ssn = e.Super_ssn
                LEFT JOIN DEPARTMENT d ON d.Dnumber = e.Dno
                WHERE e.Ssn LIKE ? OR e.Fname LIKE ? OR e.Lname LIKE ?
                   OR CONCAT(e.Fname, ' ', e.Lname) LIKE ? OR e.Address LIKE ?
                ORDER BY e.Ssn
                """;
        String p = Jdbc.like(keyword);
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee e = new Employee(
                            Jdbc.getTrimmed(rs, "Ssn"), rs.getString("Fname"),
                            Jdbc.getTrimmed(rs, "Minit"), rs.getString("Lname"));
                    e.setBdate(Jdbc.getDate(rs, "Bdate"));
                    e.setAddress(rs.getString("Address"));
                    e.setSex(Jdbc.getTrimmed(rs, "Sex"));
                    e.setSalary(rs.getBigDecimal("Salary"));
                    e.setSuperSsn(Jdbc.getTrimmed(rs, "Super_ssn"));
                    e.setDno(rs.getInt("Dno"));
                    e.setSuperName(rs.getString("SuperName"));
                    e.setDname(rs.getString("Dname"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    /** Danh sách gọn (Ssn + tên) cho các ComboBox. */
    public List<Employee> findLookup() throws SQLException {
        String sql = "SELECT Ssn, Fname, Minit, Lname FROM EMPLOYEE ORDER BY Fname, Lname";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Employee(Jdbc.getTrimmed(rs, "Ssn"), rs.getString("Fname"),
                        Jdbc.getTrimmed(rs, "Minit"), rs.getString("Lname")));
            }
        }
        return list;
    }

    public boolean exists(String ssn) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM EMPLOYEE WHERE Ssn = ?")) {
            ps.setString(1, ssn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public References countReferences(String ssn) throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM WORKS_ON   WHERE Essn = ?)      AS W,
                       (SELECT COUNT(*) FROM DEPENDENT  WHERE Essn = ?)      AS D,
                       (SELECT COUNT(*) FROM EMPLOYEE   WHERE Super_ssn = ?) AS S,
                       (SELECT COUNT(*) FROM DEPARTMENT WHERE Mgr_ssn = ?)   AS M
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, ssn);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new References(rs.getInt("W"), rs.getInt("D"), rs.getInt("S"), rs.getInt("M"));
            }
        }
    }

    // ----------------------------------------------------------- CREATE / UPDATE

    public void insert(Employee e) throws SQLException {
        String sql = """
                INSERT INTO EMPLOYEE (Fname, Minit, Lname, Bdate, Address, Sex, Salary, Super_ssn, Dno, Ssn)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        executeWrite(sql, e);
    }

    public void update(Employee e) throws SQLException {
        String sql = """
                UPDATE EMPLOYEE
                SET Fname = ?, Minit = ?, Lname = ?, Bdate = ?, Address = ?, Sex = ?,
                    Salary = ?, Super_ssn = ?, Dno = ?
                WHERE Ssn = ?
                """;
        executeWrite(sql, e);
    }

    /** INSERT và UPDATE dùng chung thứ tự tham số (Ssn ở cuối). */
    private void executeWrite(String sql, Employee e) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getFname());
            Jdbc.setString(ps, 2, e.getMinit());
            ps.setString(3, e.getLname());
            Jdbc.setDate(ps, 4, e.getBdate());
            Jdbc.setString(ps, 5, e.getAddress());
            Jdbc.setString(ps, 6, e.getSex());
            Jdbc.setDecimal(ps, 7, e.getSalary());
            Jdbc.setString(ps, 8, e.getSuperSsn());
            ps.setInt(9, e.getDno());
            ps.setString(10, e.getSsn());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * Xóa nhân viên trong một transaction:
     * xóa phân công + người phụ thuộc, gỡ quan hệ giám sát / trưởng phòng, rồi xóa nhân viên.
     * Có lỗi ở bất kỳ bước nào thì rollback toàn bộ.
     */
    public void deleteCascade(String ssn) throws SQLException {
        String[] steps = {
                "DELETE FROM WORKS_ON WHERE Essn = ?",
                "DELETE FROM DEPENDENT WHERE Essn = ?",
                "UPDATE EMPLOYEE SET Super_ssn = NULL WHERE Super_ssn = ?",
                "UPDATE DEPARTMENT SET Mgr_ssn = NULL, Mgr_start_date = NULL WHERE Mgr_ssn = ?",
                "DELETE FROM EMPLOYEE WHERE Ssn = ?"
        };
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : steps) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, ssn);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
}

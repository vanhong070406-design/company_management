# CompanyApp – Quản lý COMPANY (JavaFX + SQL Server)

Ứng dụng desktop CRUD bảng EMPLOYEE trên schema COMPANY: thêm / sửa / xóa / tìm nhân viên; chọn người giám sát và phòng ban bằng ComboBox.

Các bảng khác của schema vẫn còn trong database nhưng không có màn hình quản lý.

## Chạy lần đầu

1. **Tạo database**: mở `sql/01_schema.sql` trong SSMS → Execute.
   Chạy thêm `sql/02_sample_data.sql` để có dữ liệu mẫu theo giáo trình Elmasri (script chỉ chèn khi bảng còn trống). App không có màn hình thêm phòng ban, mà nhân viên bắt buộc thuộc một phòng, nên database trống cần có phòng ban từ script này hoặc tự thêm bằng SQL.
2. **Kết nối**: `src/main/resources/db.properties` đã đặt sẵn `sa` / `123456`, cổng 1433, database `CompanyDB`.
3. **Chạy**:
   - IntelliJ: *File → Open* → chọn thư mục `CompanyApp` → chờ Maven tải xong → mở `src/main/java/vn/company/Launcher.java` → bấm ▶ Run.
     Đừng chạy `MainApp` trực tiếp (sẽ báo *JavaFX runtime components are missing*).
   - Hoặc tab Maven bên phải: *Plugins → javafx → javafx:run*.
   - Hoặc dòng lệnh: `mvn clean javafx:run`

## Nếu không kết nối được

App sẽ hiện hộp thoại lỗi; sửa xong bấm **Kết nối lại** ở góc trên phải.

| Lỗi | Cách sửa |
|---|---|
| `The TCP/IP connection to the host localhost, port 1433 has failed` | *SQL Server Configuration Manager → SQL Server Network Configuration → Protocols for ...* → bật **TCP/IP**; tab *IP Addresses*, mục *IPAll*: xóa *TCP Dynamic Ports*, đặt *TCP Port* = `1433`. Restart dịch vụ SQL Server. |
| `Login failed for user 'sa'` | SSMS → chuột phải server → *Properties → Security* → chọn **SQL Server and Windows Authentication mode**; *Security → Logins → sa* → đặt mật khẩu, mục *Status* chọn *Enabled*. Restart dịch vụ. |
| `Cannot open database "CompanyDB"` | Chưa chạy `sql/01_schema.sql`. |
| Cài bản SQL Server Express (instance `SQLEXPRESS`) | Cách 1: đặt cổng 1433 như dòng đầu. Cách 2: đổi URL thành `jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=CompanyDB;encrypt=true;trustServerCertificate=true` và bật dịch vụ *SQL Server Browser*. |

## Ràng buộc dữ liệu được xử lý thế nào

- **Thêm nhân viên** phải chọn phòng ban (`Dno NOT NULL`), nên cần có ít nhất một phòng ban trước.
- **Xóa nhân viên** (transaction, lỗi thì rollback): xóa phân công và người phụ thuộc của họ, gỡ họ khỏi vai trò người giám sát / trưởng phòng, rồi mới xóa. Hộp thoại xác nhận liệt kê trước những gì sẽ bị ảnh hưởng.
- Khóa chính SSN bị khóa khi sửa.
- Form kiểm tra độ dài theo đúng kiểu cột: SSN đúng 9 chữ số, Fname/Lname ≤ 15, Minit 1 chữ cái, Address ≤ 50, Salary DECIMAL(10,2).

## Cấu trúc

```
sql/01_schema.sql, 02_sample_data.sql
src/main/java/vn/company/
  Launcher.java, MainApp.java
  controller/   MainController, EmployeeController
  dao/          EmployeeDAO, DepartmentDAO (chỉ đọc danh sách phòng ban), Jdbc
  model/        Employee, Department
  util/         DBConnection, Dialogs (hộp thoại + giải thích mã lỗi SQL), Fx (tiện ích giao diện)
src/main/resources/
  db.properties
  vn/company/view/  main.fxml, employee.fxml, style.css
```

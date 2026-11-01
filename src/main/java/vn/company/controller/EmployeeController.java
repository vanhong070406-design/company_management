package vn.company.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import vn.company.dao.DepartmentDAO;
import vn.company.dao.EmployeeDAO;
import vn.company.model.Department;
import vn.company.model.Employee;
import vn.company.util.Dialogs;
import vn.company.util.Fx;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Màn hình Nhân viên – bảng EMPLOYEE */
public class EmployeeController {

    /** VIEW: form chỉ đọc; ADD/EDIT: đang nhập liệu. */
    private enum Mode { VIEW, ADD, EDIT }

    private static final BigDecimal MAX_SALARY = new BigDecimal("99999999.99"); // DECIMAL(10,2)

    @FXML private TextField txtSsn, txtFname, txtMinit, txtLname, txtAddress, txtSalary, txtSearch;
    @FXML private DatePicker dpBdate;
    @FXML private RadioButton rdoMale, rdoFemale;
    @FXML private ComboBox<Employee> cboSupervisor;
    @FXML private ComboBox<Department> cboDepartment;
    @FXML private Button btnNew, btnEdit, btnDelete, btnClearSupervisor;
    @FXML private HBox viewActions, editActions;
    @FXML private Label lblFormTitle, lblFormHint;

    @FXML private TableView<Employee> table;
    @FXML private TableColumn<Employee, String> colSsn, colName, colSex, colAddress, colSupervisor, colDepartment;
    @FXML private TableColumn<Employee, LocalDate> colBdate;
    @FXML private TableColumn<Employee, BigDecimal> colSalary;
    @FXML private Label lblStatus;

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final ObservableList<Employee> data = FXCollections.observableArrayList();

    private Mode mode = Mode.VIEW;
    private List<Object> originalForm = List.of(); // giá trị form lúc bắt đầu nhập, để phát hiện thay đổi
    private boolean reloading; // đang nạp lại bảng: bỏ qua sự kiện đổi dòng chọn

    @FXML
    private void initialize() {
        ToggleGroup sex = new ToggleGroup();
        rdoMale.setToggleGroup(sex);
        rdoFemale.setToggleGroup(sex);
        rdoMale.setSelected(true);
        Fx.setupDatePicker(dpBdate);
        Fx.keepPromptText(cboSupervisor);
        Fx.keepPromptText(cboDepartment);

        Fx.bind(colSsn, Employee::getSsn);
        Fx.bind(colName, Employee::getFullName);
        Fx.bind(colBdate, Employee::getBdate);
        Fx.bind(colSex, Employee::getSex);
        Fx.bind(colAddress, Employee::getAddress);
        Fx.bind(colSalary, Employee::getSalary);
        Fx.bind(colSupervisor, Employee::getSuperName);
        Fx.bind(colDepartment, Employee::getDname);
        Fx.dateCells(colBdate);
        Fx.numberCells(colSalary, "#,##0.00");
        table.setItems(data);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, e) -> {
            if (!reloading) showSelection();
        });

        // Có thay đổi chưa lưu thì không cho đổi dòng chọn; vẫn cuộn và sắp xếp được để tra cứu
        table.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                if (hasUnsavedChanges()) {
                    ev.consume();
                    warnListLocked();
                }
            });
            return row;
        });
        table.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode().isNavigationKey() && hasUnsavedChanges()) {
                ev.consume();
                warnListLocked();
            }
        });

        viewActions.managedProperty().bind(viewActions.visibleProperty());
        editActions.managedProperty().bind(editActions.visibleProperty());
        setMode(Mode.VIEW);
    }

    // ============================================================ Tải dữ liệu

    public void refresh() {
        try {
            // Giữ nguyên lựa chọn trên form vì danh sách mới chứa các đối tượng khác
            Employee supervisor = cboSupervisor.getValue();
            Department department = cboDepartment.getValue();
            cboDepartment.setItems(FXCollections.observableArrayList(departmentDAO.findLookup()));
            cboSupervisor.setItems(FXCollections.observableArrayList(employeeDAO.findLookup()));
            Fx.select(cboSupervisor, s -> supervisor != null && s.getSsn().equals(supervisor.getSsn()));
            Fx.select(cboDepartment, d -> department != null && d.getDnumber() == department.getDnumber());
            loadTable();
        } catch (SQLException e) {
            Dialogs.sqlError("Không tải được danh sách nhân viên", e);
        }
    }

    /** Nạp lại bảng, giữ dòng đang chọn; không đụng tới dữ liệu đang nhập trên form. */
    private void loadTable() throws SQLException {
        List<Employee> list = employeeDAO.search(txtSearch.getText());
        String selectedSsn = selectedSsn();
        reloading = true;
        try {
            data.setAll(list);
            reselect(selectedSsn);
        } finally {
            reloading = false;
        }
        lblStatus.setText("Tổng số: " + list.size() + " nhân viên");
        if (mode == Mode.VIEW) showSelection();
    }

    // ============================================================== Sự kiện

    @FXML
    private void onSearch() {
        try {
            loadTable();
        } catch (SQLException e) {
            Dialogs.sqlError("Không tìm kiếm được", e);
        }
    }

    /** Làm mới danh sách từ database; không ảnh hưởng dữ liệu đang nhập trên form. */
    @FXML
    private void onReload() {
        txtSearch.clear();
        refresh();
    }

    @FXML
    private void onNew() {
        table.getSelectionModel().clearSelection();
        clearForm();
        beginInput(Mode.ADD);
        txtSsn.requestFocus();
    }

    @FXML
    private void onEdit() {
        if (table.getSelectionModel().getSelectedItem() == null) return;
        beginInput(Mode.EDIT);
        txtFname.requestFocus();
    }

    @FXML
    private void onSave() {
        Employee e = readForm();
        if (e == null) return;
        boolean adding = mode == Mode.ADD;
        try {
            if (adding) {
                if (employeeDAO.exists(e.getSsn())) {
                    Fx.markError(txtSsn);
                    Dialogs.warn("Trùng SSN", "Đã có nhân viên với SSN " + e.getSsn() + ".");
                    return;
                }
                employeeDAO.insert(e);
                txtSearch.clear(); // để chắc chắn nhìn thấy nhân viên vừa thêm trong danh sách
            } else {
                employeeDAO.update(e);
            }
            setMode(Mode.VIEW);
            refresh();
            reselect(e.getSsn());
            Dialogs.info(adding
                    ? "Đã thêm nhân viên " + e.getFullName() + "."
                    : "Đã cập nhật nhân viên " + e.getSsn() + ".");
        } catch (SQLException ex) {
            Dialogs.sqlError(adding ? "Không thêm được nhân viên" : "Không cập nhật được nhân viên", ex);
        }
    }

    @FXML
    private void onCancel() {
        if (hasUnsavedChanges()
                && !Dialogs.confirm("Hủy thay đổi", "Các thông tin vừa nhập chưa được lưu sẽ bị mất. Bạn có chắc muốn hủy?")) {
            return;
        }
        showSelection();
    }

    @FXML
    private void onDelete() {
        Employee e = table.getSelectionModel().getSelectedItem();
        if (e == null) return;
        try {
            EmployeeDAO.References ref = employeeDAO.countReferences(e.getSsn());
            String msg = "Xóa nhân viên " + e.getSsn() + " - " + e.getFullName() + "?";
            if (!ref.isEmpty()) {
                msg += "\n\nNhân viên này đang được tham chiếu. Khi xóa, hệ thống sẽ:"
                        + (ref.worksOn() > 0 ? "\n• Xóa " + ref.worksOn() + " phân công dự án (WORKS_ON)" : "")
                        + (ref.dependents() > 0 ? "\n• Xóa " + ref.dependents() + " người phụ thuộc (DEPENDENT)" : "")
                        + (ref.subordinates() > 0 ? "\n• Bỏ người giám sát của " + ref.subordinates() + " nhân viên cấp dưới" : "")
                        + (ref.managedDepartments() > 0 ? "\n• Bỏ chức trưởng phòng ở " + ref.managedDepartments() + " phòng ban" : "");
            }
            if (!Dialogs.confirm("Xác nhận xóa", msg)) return;

            employeeDAO.deleteCascade(e.getSsn());
            refresh();
            Dialogs.info("Đã xóa nhân viên " + e.getSsn() + ".");
        } catch (SQLException ex) {
            Dialogs.sqlError("Không xóa được nhân viên", ex);
        }
    }

    @FXML
    private void onClearSupervisor() {
        cboSupervisor.setValue(null);
    }

    // ========================================================= Form <-> model

    private void showOnForm(Employee e) {
        Fx.clearErrors(txtSsn, txtFname, txtMinit, txtLname, txtAddress, txtSalary, dpBdate, cboDepartment);
        txtSsn.setText(e.getSsn());
        txtFname.setText(e.getFname());
        txtMinit.setText(e.getMinit());
        txtLname.setText(e.getLname());
        dpBdate.setValue(e.getBdate());
        // setValue không cập nhật ô nhập nếu giá trị không đổi, nên chữ gõ dở phải được ghi đè
        dpBdate.getEditor().setText(e.getBdate() == null ? "" : Fx.DATE.format(e.getBdate()));
        rdoMale.setSelected(!"F".equalsIgnoreCase(e.getSex()));
        rdoFemale.setSelected("F".equalsIgnoreCase(e.getSex()));
        txtAddress.setText(e.getAddress());
        txtSalary.setText(e.getSalary() == null ? "" : e.getSalary().toPlainString());
        Fx.select(cboSupervisor, s -> s.getSsn().equals(e.getSuperSsn()));
        Fx.select(cboDepartment, d -> e.getDno() != null && d.getDnumber() == e.getDno());
    }

    /** Đọc và kiểm tra dữ liệu trên form; trả về null nếu có lỗi. */
    private Employee readForm() {
        Fx.clearErrors(txtSsn, txtFname, txtMinit, txtLname, txtAddress, txtSalary, dpBdate, cboDepartment);
        List<String> errors = new ArrayList<>();

        String ssn = Fx.text(txtSsn);
        if (ssn == null || !ssn.matches("\\d{9}")) {
            errors.add("SSN phải gồm đúng 9 chữ số.");
            Fx.markError(txtSsn);
        }

        String fname = Fx.text(txtFname);
        if (fname == null || fname.length() > 15) {
            errors.add("Tên (Fname) bắt buộc, tối đa 15 ký tự.");
            Fx.markError(txtFname);
        }

        String minit = Fx.text(txtMinit);
        if (minit != null && !minit.matches("[A-Za-z]")) {
            errors.add("Chữ lót (Minit) chỉ gồm 1 chữ cái.");
            Fx.markError(txtMinit);
        }

        String lname = Fx.text(txtLname);
        if (lname == null || lname.length() > 15) {
            errors.add("Họ (Lname) bắt buộc, tối đa 15 ký tự.");
            Fx.markError(txtLname);
        }

        LocalDate bdate = null;
        try {
            bdate = Fx.readDate(dpBdate);
            if (bdate != null && bdate.isAfter(LocalDate.now())) {
                errors.add("Ngày sinh không được ở tương lai.");
                Fx.markError(dpBdate);
            }
        } catch (IllegalArgumentException ex) {
            errors.add("Ngày sinh phải có dạng dd/MM/yyyy.");
            Fx.markError(dpBdate);
        }

        String address = Fx.text(txtAddress);
        if (address != null && address.length() > 50) {
            errors.add("Địa chỉ tối đa 50 ký tự.");
            Fx.markError(txtAddress);
        }

        BigDecimal salary = null;
        String salaryText = Fx.text(txtSalary);
        if (salaryText != null) {
            try {
                salary = new BigDecimal(salaryText.replace(",", ""));
                if (salary.signum() < 0 || salary.compareTo(MAX_SALARY) > 0 || salary.scale() > 2) {
                    errors.add("Lương phải từ 0 đến 99,999,999.99 và tối đa 2 chữ số thập phân.");
                    Fx.markError(txtSalary);
                }
            } catch (NumberFormatException ex) {
                errors.add("Lương phải là số.");
                Fx.markError(txtSalary);
            }
        }

        Employee supervisor = cboSupervisor.getValue();
        if (supervisor != null && supervisor.getSsn().equals(ssn)) {
            errors.add("Nhân viên không thể tự giám sát chính mình.");
        }

        Department dept = cboDepartment.getValue();
        if (dept == null) {
            errors.add("Phải chọn phòng ban.");
            Fx.markError(cboDepartment);
        }

        if (!errors.isEmpty()) {
            Dialogs.validation(errors);
            return null;
        }

        Employee e = new Employee(ssn, fname, minit == null ? null : minit.toUpperCase(), lname);
        e.setBdate(bdate);
        e.setSex(rdoFemale.isSelected() ? "F" : "M");
        e.setAddress(address);
        e.setSalary(salary);
        e.setSuperSsn(supervisor == null ? null : supervisor.getSsn());
        e.setDno(dept.getDnumber());
        return e;
    }

    private void clearForm() {
        Fx.clearErrors(txtSsn, txtFname, txtMinit, txtLname, txtAddress, txtSalary, dpBdate, cboDepartment);
        for (TextField tf : List.of(txtSsn, txtFname, txtMinit, txtLname, txtAddress, txtSalary)) tf.clear();
        Fx.clearDate(dpBdate);
        rdoMale.setSelected(true);
        cboSupervisor.setValue(null);
        cboDepartment.setValue(null);
    }

    /** Các giá trị hiện tại trên form, dùng để so sánh xem người dùng đã nhập gì chưa. */
    private List<Object> formValues() {
        Employee supervisor = cboSupervisor.getValue();
        Department department = cboDepartment.getValue();
        return Arrays.asList(
                Fx.text(txtSsn), Fx.text(txtFname), Fx.text(txtMinit), Fx.text(txtLname),
                Fx.text(dpBdate.getEditor()), rdoFemale.isSelected(),
                Fx.text(txtAddress), Fx.text(txtSalary),
                supervisor == null ? null : supervisor.getSsn(),
                department == null ? null : department.getDnumber());
    }

    // ========================================================= Trạng thái form

    /** Về chế độ xem: hiển thị dòng đang chọn, hoặc form trống nếu không chọn dòng nào. */
    private void showSelection() {
        Employee e = table.getSelectionModel().getSelectedItem();
        if (e != null) showOnForm(e);
        else clearForm();
        setMode(Mode.VIEW);
    }

    private void beginInput(Mode inputMode) {
        originalForm = formValues();
        setMode(inputMode);
    }

    private boolean hasUnsavedChanges() {
        return mode != Mode.VIEW && !formValues().equals(originalForm);
    }

    private void setMode(Mode newMode) {
        mode = newMode;
        boolean viewing = mode == Mode.VIEW;
        boolean selected = table.getSelectionModel().getSelectedItem() != null;

        // SSN là khóa chính nên chỉ được nhập khi thêm mới
        txtSsn.setDisable(mode != Mode.ADD);
        for (Control c : List.of(txtFname, txtMinit, txtLname, dpBdate, rdoMale, rdoFemale,
                txtAddress, txtSalary, cboSupervisor, btnClearSupervisor, cboDepartment)) {
            c.setDisable(viewing);
        }

        viewActions.setVisible(viewing);
        editActions.setVisible(!viewing);
        btnNew.setDisable(!viewing);
        btnEdit.setDisable(!selected);
        btnDelete.setDisable(!selected);

        lblFormHint.getStyleClass().remove("form-hint-warning");
        switch (mode) {
            case ADD -> {
                lblFormTitle.setText("Thêm nhân viên mới");
                lblFormHint.setText("Nhập thông tin rồi bấm Lưu, hoặc Hủy để bỏ qua.");
            }
            case EDIT -> {
                lblFormTitle.setText("Sửa nhân viên " + Fx.text(txtSsn));
                lblFormHint.setText("Chỉnh sửa thông tin rồi bấm Lưu, hoặc Hủy để bỏ qua.");
            }
            case VIEW -> {
                lblFormTitle.setText("Thông tin nhân viên");
                lblFormHint.setText(selected
                        ? "Bấm Sửa để thay đổi thông tin nhân viên này."
                        : "Chọn một nhân viên trong danh sách, hoặc bấm Thêm mới.");
            }
        }
    }

    private void warnListLocked() {
        lblFormHint.setText("Bạn có thay đổi chưa lưu. Hãy bấm Lưu hoặc Hủy trước khi chọn nhân viên khác.");
        if (!lblFormHint.getStyleClass().contains("form-hint-warning")) {
            lblFormHint.getStyleClass().add("form-hint-warning");
        }
    }

    // ============================================================== Tiện ích

    private String selectedSsn() {
        Employee e = table.getSelectionModel().getSelectedItem();
        return e == null ? null : e.getSsn();
    }

    private void reselect(String ssn) {
        if (ssn == null) return;
        data.stream().filter(e -> e.getSsn().equals(ssn)).findFirst().ifPresent(e -> {
            table.getSelectionModel().select(e);
            table.scrollTo(e);
        });
    }
}

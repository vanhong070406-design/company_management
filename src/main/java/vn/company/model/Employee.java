package vn.company.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Bảng EMPLOYEE */
public class Employee {
    private String ssn;
    private String fname;
    private String minit;
    private String lname;
    private LocalDate bdate;
    private String address;
    private String sex;
    private BigDecimal salary;
    private String superSsn;
    private Integer dno;

    // Cột hiển thị lấy từ JOIN
    private String superName;
    private String dname;

    public Employee() {
    }

    public Employee(String ssn, String fname, String minit, String lname) {
        this.ssn = ssn;
        this.fname = fname;
        this.minit = minit;
        this.lname = lname;
    }

    /** VD: "John B. Smith" */
    public String getFullName() {
        StringBuilder sb = new StringBuilder(fname == null ? "" : fname);
        if (minit != null && !minit.isBlank()) sb.append(' ').append(minit.trim()).append('.');
        if (lname != null) sb.append(' ').append(lname);
        return sb.toString();
    }

    /** Dùng để hiển thị trong ComboBox */
    @Override
    public String toString() {
        return ssn + " - " + getFullName();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Employee e && Objects.equals(ssn, e.ssn);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ssn);
    }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }
    public String getMinit() { return minit; }
    public void setMinit(String minit) { this.minit = minit; }
    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }
    public LocalDate getBdate() { return bdate; }
    public void setBdate(LocalDate bdate) { this.bdate = bdate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public String getSuperSsn() { return superSsn; }
    public void setSuperSsn(String superSsn) { this.superSsn = superSsn; }
    public Integer getDno() { return dno; }
    public void setDno(Integer dno) { this.dno = dno; }
    public String getSuperName() { return superName; }
    public void setSuperName(String superName) { this.superName = superName; }
    public String getDname() { return dname; }
    public void setDname(String dname) { this.dname = dname; }
}

package vn.company.model;

/** Bảng DEPARTMENT, chỉ dùng để chọn phòng ban cho nhân viên. */
public class Department {
    private final int dnumber;
    private final String dname;

    public Department(int dnumber, String dname) {
        this.dnumber = dnumber;
        this.dname = dname;
    }

    @Override
    public String toString() {
        return dnumber + " - " + dname;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Department d && d.dnumber == dnumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(dnumber);
    }

    public int getDnumber() { return dnumber; }
    public String getDname() { return dname; }
}

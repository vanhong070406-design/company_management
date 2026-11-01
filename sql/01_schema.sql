-- Tạo cơ sở dữ liệu Company nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'CompanyDB')
BEGIN
    CREATE DATABASE CompanyDB;
END;
GO

USE CompanyDB;
GO

-- 1. Create DEPARTMENT table if it does not exist
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DEPARTMENT')
BEGIN
    CREATE TABLE DEPARTMENT (
        Dname VARCHAR(50) NOT NULL,
        Dnumber INT NOT NULL,
        Mgr_ssn CHAR(9),
        Mgr_start_date DATE,
        CONSTRAINT PK_DEPARTMENT PRIMARY KEY (Dnumber)
    );
END;

-- 2. Create EMPLOYEE table if it does not exist
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'EMPLOYEE')
BEGIN
    CREATE TABLE EMPLOYEE (
        Fname VARCHAR(15) NOT NULL,
        Minit CHAR(1),
        Lname VARCHAR(15) NOT NULL,
        Ssn CHAR(9) NOT NULL,
        Bdate DATE,
        Address VARCHAR(50),
        Sex CHAR(1),
        Salary DECIMAL(10, 2),
        Super_ssn CHAR(9),
        Dno INT NOT NULL,
        CONSTRAINT PK_EMPLOYEE PRIMARY KEY (Ssn),
        CONSTRAINT FK_EMPLOYEE_SUPERVISOR FOREIGN KEY (Super_ssn) REFERENCES EMPLOYEE(Ssn),
        CONSTRAINT FK_EMPLOYEE_DEPARTMENT FOREIGN KEY (Dno) REFERENCES DEPARTMENT(Dnumber)
    );
END;

-- Add foreign key Mgr_ssn to DEPARTMENT if it hasn't been created yet
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'DEPARTMENT')
   AND NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_DEPARTMENT_MANAGER')
BEGIN
    ALTER TABLE DEPARTMENT
    ADD CONSTRAINT FK_DEPARTMENT_MANAGER
    FOREIGN KEY (Mgr_ssn) REFERENCES EMPLOYEE(Ssn);
END;

-- 3. Create DEPT_LOCATIONS table if it does not exist
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DEPT_LOCATIONS')
BEGIN
    CREATE TABLE DEPT_LOCATIONS (
        Dnumber INT NOT NULL,
        Dlocation VARCHAR(15) NOT NULL,
        CONSTRAINT PK_DEPT_LOCATIONS PRIMARY KEY (Dnumber, Dlocation),
        CONSTRAINT FK_DEPT_LOCATIONS_DEPARTMENT FOREIGN KEY (Dnumber) REFERENCES DEPARTMENT(Dnumber)
    );
END;

-- 4. Create PROJECT table if it does not exist
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PROJECT')
BEGIN
    CREATE TABLE PROJECT (
        Pname VARCHAR(50) NOT NULL,
        Pnumber INT NOT NULL,
        Plocation VARCHAR(15),
        Dnum INT NOT NULL,
        CONSTRAINT PK_PROJECT PRIMARY KEY (Pnumber),
        CONSTRAINT FK_PROJECT_DEPARTMENT FOREIGN KEY (Dnum) REFERENCES DEPARTMENT(Dnumber)
    );
END;

-- 5. Create WORKS_ON table if it does not exist
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'WORKS_ON')
BEGIN
    CREATE TABLE WORKS_ON (
        Essn CHAR(9) NOT NULL,
        Pno INT NOT NULL,
        Hours DECIMAL(3, 1) NULL,
        CONSTRAINT PK_WORKS_ON PRIMARY KEY (Essn, Pno),
        CONSTRAINT FK_WORKS_ON_EMPLOYEE FOREIGN KEY (Essn) REFERENCES EMPLOYEE(Ssn),
        CONSTRAINT FK_WORKS_ON_PROJECT FOREIGN KEY (Pno) REFERENCES PROJECT(Pnumber)
    );
END;

-- 6. Create DEPENDENT table if it does not exist
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DEPENDENT')
BEGIN
    CREATE TABLE DEPENDENT (
        Essn CHAR(9) NOT NULL,
        Dependent_name VARCHAR(15) NOT NULL,
        Sex CHAR(1),
        Bdate DATE,
        Relationship VARCHAR(15),
        CONSTRAINT PK_DEPENDENT PRIMARY KEY (Essn, Dependent_name),
        CONSTRAINT FK_DEPENDENT_EMPLOYEE FOREIGN KEY (Essn) REFERENCES EMPLOYEE(Ssn)
    );
END;
GO

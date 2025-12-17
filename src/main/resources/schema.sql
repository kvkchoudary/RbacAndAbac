CREATE TABLE FID (
  fid_name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE ROLE (
  role_name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE FID_ROLE (
  fid_name VARCHAR(50),
  role_name VARCHAR(50)
);

CREATE TABLE ROLE_TABLE_PERMISSION (
  role_name VARCHAR(50),
  table_name VARCHAR(50),
  permission VARCHAR(10)
);

CREATE TABLE ROLE_COLUMN_PERMISSION (
  role_name VARCHAR(50),
  table_name VARCHAR(50),
  column_name VARCHAR(50),
  permission VARCHAR(10)
);

CREATE TABLE ROLE_ROW_RULE (
  role_name VARCHAR(50),
  table_name VARCHAR(50),
  permission VARCHAR(10),
  rule_expression VARCHAR(200)
);

CREATE TABLE EMPLOYEE (
  emp_id INT,
  name VARCHAR(50),
  department VARCHAR(50),
  salary INT,
  dept_id INT
);

CREATE TABLE DEPARTMENT (
  dept_id INT,
  dept_name VARCHAR(50),
  region VARCHAR(50)
);

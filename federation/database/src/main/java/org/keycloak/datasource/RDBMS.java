package org.keycloak.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RDBMS {
//    MYSQL("MySQL 5.7+", "com.mysql.cj.jdbc.Driver", "SELECT 1"),
    MARIADB("MariaDB 10.0+", "org.mariadb.jdbc.Driver", "SELECT 1");
//    POSTGRESQL("PostgreSQL 9+", org.postgresql.Driver.class.getName(), "SELECT 1", new PostgreSQL9Dialect()),
//    ORACLE("Oracle 12+", oracle.jdbc.OracleDriver.class.getName(), "SELECT 1 FROM DUAL", new Oracle12cDialect()),
//    IBMDB2("IBM DB2", com.ibm.db2.jcc.DB2Driver.class.getName(), "select * from sysibm.sysdummy1", new DB2Dialect()),
//    SQL_SERVER("MS SQL Server 2012+ (jtds)", net.sourceforge.jtds.jdbc.Driver.class.getName(), "SELECT 1", new SQLServer2012Dialect());

    private final String  desc;
    private final String  driver;
    private final String  testString;

    RDBMS(String desc, String driver, String testString) {
        this.desc = desc;
        this.driver = driver;
        this.testString = testString;
    }

    public static RDBMS getByDescription(String desc) {
        for (RDBMS value : values()) {
            if (value.desc.equals(desc)) {
                return value;
            }
        }
        return null;
    }

    public static List<String> getAllDescriptions() {
        return Arrays.stream(values()).map(RDBMS::getDesc).collect(Collectors.toList());
    }

    public String getDesc() {
        return desc;
    }

    public String getDriver() {
        return driver;
    }

    public String getTestString() {
        return testString;
    }
}


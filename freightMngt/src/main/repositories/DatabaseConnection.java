package main.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection utility for Oracle database
 */
public class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@localhost:1521:XE";

    private static Connection connection;

    /**
     * Get a database connection using default settings
     * Note: This method requires credentials to be provided via the overloaded method
     * @deprecated Use getConnection(String url, String user, String password, boolean asSysdba) instead
     */
    @Deprecated
    public static Connection getConnection() throws SQLException {
        throw new SQLException("Please use getConnection(url, user, password, asSysdba) to provide credentials");
    }

    /**
     * Get a database connection with custom credentials
     */
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        // Default to SYSDBA if user is 'sys'
        return getConnection(url, user, password, "sys".equalsIgnoreCase(user));
    }

    /**
     * Get a database connection with custom credentials and SYSDBA option
     */
    public static Connection getConnection(String url, String user, String password, boolean asSysdba) throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Try newer driver class first (Oracle 12c+)
                try {
                    Class.forName("oracle.jdbc.OracleDriver");
                } catch (ClassNotFoundException e) {
                    // Fall back to older driver class (Oracle 11g and earlier)
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                }
                
                Connection conn;
                if (asSysdba && "sys".equalsIgnoreCase(user)) {
                    // Connect as SYSDBA using Properties
                    Properties props = new Properties();
                    props.put("user", user);
                    props.put("password", password);
                    props.put("internal_logon", "sysdba");
                    conn = DriverManager.getConnection(url, props);
                } else {
                    // Normal connection
                    conn = DriverManager.getConnection(url, user, password);
                }
                
                conn.setAutoCommit(false);
                connection = conn;
            } catch (ClassNotFoundException e) {
                throw new SQLException("Oracle JDBC Driver not found. Please add ojdbc8.jar or ojdbc11.jar to your classpath.", e);
            }
        }
        return connection;
    }

    /**
     * Close the database connection
     */
    public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Test the database connection with provided credentials
     */
    public static boolean testConnection(String url, String user, String password, boolean asSysdba) {
        try {
            Connection conn = getConnection(url, user, password, asSysdba);
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}


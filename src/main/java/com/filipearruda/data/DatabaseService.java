package com.filipearruda.data;

import com.filipearruda.dto.FeedbackRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseService.class.getName());

    private final String url;
    private final String user;
    private final String password;

    public DatabaseService() {
        this(
                getEnv("DB_URL", getEnv("SQLSERVER_URL", getEnv("MSSQL_URL", "jdbc:sqlserver://localhost:1433;databaseName=feedback_db;encrypt=true;trustServerCertificate=true;"))),
                getEnv("DB_USER", getEnv("SQLSERVER_USER", getEnv("MSSQL_USER", "sa"))),
                getEnv("DB_PASSWORD", getEnv("SQLSERVER_PASSWORD", getEnv("MSSQL_PASSWORD", "password")))
        );
    }

    public DatabaseService(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initDatabase() {
        String createTableSql = "IF OBJECT_ID(N'feedbacks', N'U') IS NULL " +
                "CREATE TABLE feedbacks (" +
                "id INT IDENTITY(1,1) PRIMARY KEY, " +
                "description NVARCHAR(MAX) NOT NULL, " +
                "rate INT NOT NULL, " +
                "created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            LOGGER.info("Feedbacks table initialized successfully (if not existed).");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not initialize database table: " + e.getMessage(), e);
        }
    }

    public int saveFeedback(FeedbackRequest feedback) throws SQLException {
        String sql = "INSERT INTO feedbacks (description, rate) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, feedback.getDescription());
            stmt.setInt(2, feedback.getRate());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating feedback failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    LOGGER.info("Feedback persisted successfully with ID: " + generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating feedback failed, no ID obtained.");
                }
            }
        }
    }
}

package com.filipearruda;

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
                getEnv("DB_URL", getEnv("POSTGRES_URL", "jdbc:postgresql://localhost:5432/feedback_db")),
                getEnv("DB_USER", getEnv("POSTGRES_USER", "postgres")),
                getEnv("DB_PASSWORD", getEnv("POSTGRES_PASSWORD", "password"))
        );
    }

    public DatabaseService(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
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
        String createTableSql = "CREATE TABLE IF NOT EXISTS feedbacks (" +
                "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "description TEXT NOT NULL, " +
                "rate INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
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

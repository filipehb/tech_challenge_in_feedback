package com.filipearruda;

import com.filipearruda.data.DatabaseService;
import com.filipearruda.dto.FeedbackRequest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseServiceTest {

    @Test
    void testDefaultConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service.getUrl());
        assertTrue(service.getUrl().contains("sqlserver"));
        assertNotNull(service.getUser());
        assertNotNull(service.getPassword());
    }

    @Test
    void testParameterizedConstructor() {
        DatabaseService service = new DatabaseService("jdbc:sqlserver://myhost:1433;databaseName=testdb", "myuser", "mypass");
        assertEquals("jdbc:sqlserver://myhost:1433;databaseName=testdb", service.getUrl());
        assertEquals("myuser", service.getUser());
        assertEquals("mypass", service.getPassword());
    }

    @Test
    void testInitDatabaseExecutesTSQLStatement() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        DatabaseService service = new DatabaseService("jdbc:sqlserver://localhost:1433;databaseName=feedback_db", "sa", "password") {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        };

        service.initDatabase();

        verify(mockStatement).execute(org.mockito.ArgumentMatchers.contains("IF OBJECT_ID(N'feedbacks', N'U') IS NULL"));
    }

    @Test
    void testSaveFeedbackSuccess() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(42);

        DatabaseService service = new DatabaseService("jdbc:sqlserver://localhost:1433;databaseName=feedback_db", "sa", "password") {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        };

        FeedbackRequest request = new FeedbackRequest("Great service", 10);
        int generatedId = service.saveFeedback(request);

        assertEquals(42, generatedId);
        verify(mockPreparedStatement).setString(1, "Great service");
        verify(mockPreparedStatement).setInt(2, 10);
    }

    @Test
    void testSaveFeedbackNoRowsAffectedThrowsException() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        DatabaseService service = new DatabaseService("jdbc:sqlserver://localhost:1433;databaseName=feedback_db", "sa", "password") {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        };

        FeedbackRequest request = new FeedbackRequest("Bad response", 1);
        assertThrows(SQLException.class, () -> service.saveFeedback(request));
    }

    @Test
    void testSaveFeedbackNoGeneratedKeyThrowsException() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        DatabaseService service = new DatabaseService("jdbc:sqlserver://localhost:1433;databaseName=feedback_db", "sa", "password") {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        };

        FeedbackRequest request = new FeedbackRequest("Test", 5);
        assertThrows(SQLException.class, () -> service.saveFeedback(request));
    }
}

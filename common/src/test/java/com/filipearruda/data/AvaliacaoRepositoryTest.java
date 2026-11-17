package com.filipearruda.data;

import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvaliacaoRepositoryTest {

    @Test
    void padraoApontaParaSqlServer() {
        AvaliacaoRepository repository = new AvaliacaoRepository();
        assertTrue(repository.getUrl().contains("sqlserver"));
    }

    @Test
    void initCriaATabelaDeAvaliacoes() throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        repository(connection).init();

        verify(statement).execute(contains("CREATE TABLE avaliacoes"));
    }

    @Test
    void saveGravaEDevolveOId() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet keys = mock(ResultSet.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(42L);

        Avaliacao salva = repository(connection).save(new Avaliacao(
                0, "nota 1", 1, Urgencia.ALTA, Instant.parse("2026-11-17T20:00:00Z")));

        assertEquals(42L, salva.id());
        verify(statement).setString(1, "nota 1");
        verify(statement).setInt(2, 1);
        verify(statement).setString(3, "ALTA");
        verify(statement).setString(eq(4), contains("2026-11-17T20:00:00"));
    }

    @Test
    void saveSemLinhaFalha() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);

        assertThrows(SQLException.class, () -> repository(connection).save(
                new Avaliacao(0, "x", 1, Urgencia.ALTA, Instant.parse("2026-11-17T20:00:00Z"))));
    }

    @Test
    void interpretaDataUtcGravadaPeloSqlServer() {
        assertEquals(Instant.parse("2026-11-17T20:00:00Z"), AvaliacaoRepository.parseUtc("2026-11-17 20:00:00.0000000"));
    }

    private static AvaliacaoRepository repository(Connection connection) {
        return new AvaliacaoRepository("jdbc:sqlserver://localhost", "sa", "password") {
            @Override
            public Connection getConnection() {
                return connection;
            }
        };
    }
}

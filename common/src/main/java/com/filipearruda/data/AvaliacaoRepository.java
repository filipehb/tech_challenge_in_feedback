package com.filipearruda.data;

import com.filipearruda.domain.Avaliacao;
import com.filipearruda.domain.Urgencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

public class AvaliacaoRepository {
    private static final DateTimeFormatter SQL_UTC = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private static final String CREATE_TABLE = """
            IF OBJECT_ID(N'avaliacoes', N'U') IS NULL
            CREATE TABLE avaliacoes (
                id INT IDENTITY(1,1) PRIMARY KEY,
                descricao NVARCHAR(MAX) NOT NULL,
                nota INT NOT NULL,
                urgencia NVARCHAR(10) NOT NULL,
                data_envio DATETIME2 NOT NULL
            );
            """;

    private static final String MIGRATE_LEGACY = """
            IF OBJECT_ID(N'feedbacks', N'U') IS NOT NULL
            INSERT INTO avaliacoes (descricao, nota, urgencia, data_envio)
            SELECT f.description,
                   f.rate,
                   CASE WHEN f.rate <= 3 THEN N'ALTA' WHEN f.rate <= 6 THEN N'MEDIA' ELSE N'BAIXA' END,
                   f.created_at
            FROM feedbacks f
            WHERE NOT EXISTS (
                SELECT 1 FROM avaliacoes a
                WHERE a.descricao = f.description
                  AND a.nota = f.rate
                  AND a.data_envio = f.created_at
            );
            """;

    private final String url;
    private final String user;
    private final String password;

    public AvaliacaoRepository() {
        this(
                getenv("DB_URL", "jdbc:sqlserver://localhost:1433;databaseName=feedback_db;encrypt=true;trustServerCertificate=true;"),
                getenv("DB_USER", "sa"),
                getenv("DB_PASSWORD", "password")
        );
    }

    public AvaliacaoRepository(String url, String user, String password) {
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

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void init() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
            statement.execute(MIGRATE_LEGACY);
        }
    }

    public Avaliacao save(Avaliacao avaliacao) throws SQLException {
        String sql = "INSERT INTO avaliacoes (descricao, nota, urgencia, data_envio) VALUES (?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, avaliacao.descricao());
            statement.setInt(2, avaliacao.nota());
            statement.setString(3, avaliacao.urgencia().name());
            statement.setString(4, SQL_UTC.format(avaliacao.dataEnvio()));

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Nenhuma linha foi gravada.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("O banco não devolveu o id da avaliação.");
                }
                return new Avaliacao(keys.getLong(1), avaliacao.descricao(), avaliacao.nota(), avaliacao.urgencia(), avaliacao.dataEnvio());
            }
        }
    }

    public List<Avaliacao> listSince(Instant since) throws SQLException {
        String sql = """
                SELECT id, descricao, nota, urgencia, data_envio
                FROM avaliacoes
                WHERE data_envio >= ?
                ORDER BY data_envio ASC
                """;
        List<Avaliacao> avaliacoes = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SQL_UTC.format(since));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    avaliacoes.add(new Avaliacao(
                            rows.getLong("id"),
                            rows.getString("descricao"),
                            rows.getInt("nota"),
                            Urgencia.valueOf(rows.getString("urgencia")),
                            parseUtc(rows.getString("data_envio"))
                    ));
                }
            }
        }
        return avaliacoes;
    }

    static Instant parseUtc(String value) {
        String normalized = value.trim().replace(' ', 'T');
        int zone = Math.max(normalized.indexOf('Z'), normalized.indexOf('+'));
        if (zone > 0) {
            normalized = normalized.substring(0, zone);
        }
        int dot = normalized.indexOf('.');
        if (dot >= 0) {
            String fraction = normalized.substring(dot + 1).replaceAll("\\D", "");
            fraction = (fraction + "000").substring(0, 3);
            normalized = normalized.substring(0, dot) + "." + fraction;
            LocalDateTime dateTime = LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            return dateTime.toInstant(ZoneOffset.UTC);
        }
        return LocalDateTime.parse(normalized).toInstant(ZoneOffset.UTC);
    }

    private static String getenv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}

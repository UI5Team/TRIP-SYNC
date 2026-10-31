package com.tripsync.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String SCHEMA_RESOURCE = "/com/tripsync/db/schema.sql";
    private static final String FOLDER_NAME = "TripSync";
    private static final String FILE_NAME = "tripsync.db";

    private static Connection connection;
    private static Path databasePath;

    private Database() {
    }

    public static void init() {
        if (connection != null) {
            return;
        }
        try {
            databasePath = resolveDatabasePath();
            Files.createDirectories(databasePath.getParent());

            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }

            applySchema();

        } catch (SQLException | IOException e) {
            throw new DataAccessException(
                    "데이터베이스를 준비하지 못했습니다: " + databasePath, e);
        }
    }

    public static Connection connection() {
        if (connection == null) {
            throw new IllegalStateException("Database.init() 이 먼저 호출되어야 합니다.");
        }
        return connection;
    }

    public static void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("[경고] 데이터베이스를 닫는 중 오류: " + e.getMessage());
        } finally {
            connection = null;
        }
    }

    public static Path path() {
        return databasePath;
    }

    private static Path resolveDatabasePath() {
        return Path.of(System.getProperty("user.home"), FOLDER_NAME, FILE_NAME);
    }

    private static void applySchema() throws SQLException {
        String sql = readSchema();

        try (Statement statement = connection.createStatement()) {

            for (String rawStatement : stripComments(sql).split(";")) {
                String trimmed = rawStatement.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new DataAccessException("스키마 파일을 찾을 수 없습니다: " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("스키마 파일을 읽지 못했습니다: " + SCHEMA_RESOURCE, e);
        }
    }

    private static String stripComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\\R")) {
            int comment = line.indexOf("--");
            out.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        return out.toString();
    }
}

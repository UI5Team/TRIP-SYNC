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

/**
 * SQLite 연결과 스키마를 관리한다.
 *
 * <p><b>왜 SQLite 인가</b> — 설계서 5장은 서버 없이 로컬에 저장하라고 정하고 있다.
 * SQLite 는 별도 서버 프로세스 없이 {@code .db} 파일 하나로 동작하므로
 * 인터넷이 끊긴 환경에서도 그대로 돌아가고, SQL 로 데이터 구조를 설명할 수 있다.</p>
 *
 * <p><b>연결을 하나만 쓰는 이유</b> — 데스크톱 앱은 사용자가 한 명이고 모든 DB 작업이
 * JavaFX 애플리케이션 스레드에서 순차적으로 일어난다. 커넥션 풀은 이 상황에서
 * 복잡도만 늘리므로, 연결 하나를 열어두고 재사용한다.</p>
 *
 * <p><b>저장 위치</b> — 사용자 홈 폴더 아래 {@code TripSync/tripsync.db}.
 * 프로젝트 폴더가 아니라 홈에 두는 이유는, 실행 위치(IDE / 터미널 / jar)에 따라
 * 작업 디렉터리가 달라져도 항상 같은 파일을 쓰게 하기 위해서다.
 * Windows 기준 {@code C:\Users\사용자명\TripSync\tripsync.db} 에 생긴다.</p>
 */
public final class Database {

    private static final String SCHEMA_RESOURCE = "/com/tripsync/db/schema.sql";
    private static final String FOLDER_NAME = "TripSync";
    private static final String FILE_NAME = "tripsync.db";

    private static Connection connection;
    private static Path databasePath;

    private Database() {
    }

    /**
     * 앱 시작 시 한 번 호출한다. DB 파일을 만들고 스키마를 적용한다.
     *
     * @throws DataAccessException 연결이나 스키마 적용에 실패한 경우
     */
    public static void init() {
        if (connection != null) {
            return;
        }
        try {
            databasePath = resolveDatabasePath();
            Files.createDirectories(databasePath.getParent());

            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            // 외래 키 제약은 연결마다 켜야 한다 (SQLite 기본값은 OFF)
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }

            applySchema();

        } catch (SQLException | IOException e) {
            throw new DataAccessException(
                    "데이터베이스를 준비하지 못했습니다: " + databasePath, e);
        }
    }

    /** 열려 있는 연결을 돌려준다. {@link #init()} 이 먼저 호출돼 있어야 한다. */
    public static Connection connection() {
        if (connection == null) {
            throw new IllegalStateException("Database.init() 이 먼저 호출되어야 합니다.");
        }
        return connection;
    }

    /** 앱 종료 시 호출한다. */
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

    /** DB 파일 경로. 설정 화면이나 오류 안내에서 사용자에게 보여줄 때 쓴다. */
    public static Path path() {
        return databasePath;
    }

    private static Path resolveDatabasePath() {
        return Path.of(System.getProperty("user.home"), FOLDER_NAME, FILE_NAME);
    }

    /** schema.sql 을 읽어 실행한다. 모든 문장이 멱등이라 매 실행마다 돌려도 안전하다. */
    private static void applySchema() throws SQLException {
        String sql = readSchema();

        try (Statement statement = connection.createStatement()) {
            // 주석을 걷어내고 세미콜론 단위로 끊어 실행한다.
            // schema.sql 에는 문자열 리터럴이나 트리거가 없으므로 이 단순한 분리로 충분하다.
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

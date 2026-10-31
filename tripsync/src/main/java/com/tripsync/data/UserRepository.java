package com.tripsync.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tripsync.model.AppUser;

/**
 * 로컬 프로필 저장소.
 *
 * <p>"지금 누가 앱을 쓰고 있는가"를 {@code app_setting} 테이블에 한 줄로 기억해 둔다.
 * 프로필을 바꾸면 모임 목록이 그 사람이 참여한 모임으로 바뀌므로,
 * 한 PC 에서도 초대 코드로 모임에 들어가는 과정을 끝까지 보여줄 수 있다.</p>
 */
public final class UserRepository {

    /** 처음 실행했을 때 자동으로 만들어지는 기본 프로필 이름 */
    private static final String DEFAULT_USER_NAME = "나";

    /** app_setting 에서 현재 프로필을 가리키는 키 */
    private static final String CURRENT_USER_KEY = "currentUserId";

    private UserRepository() {
    }

    /**
     * 프로필이 하나도 없으면 기본 프로필을 만들고 현재 사용자로 지정한다.
     * 앱 시작 시 한 번 호출한다.
     */
    public static void ensureDefaultUser() {
        if (findAll().isEmpty()) {
            AppUser created = insert(DEFAULT_USER_NAME);
            setCurrentUser(created.id());
            return;
        }
        // 프로필은 있는데 선택된 사용자가 없거나 지워진 경우 첫 번째로 되돌린다
        if (currentUser().isEmpty()) {
            setCurrentUser(findAll().get(0).id());
        }
    }

    /** 모든 프로필을 이름순으로 돌려준다. */
    public static List<AppUser> findAll() {
        String sql = "SELECT user_id, display_name FROM app_user ORDER BY user_id";
        List<AppUser> users = new ArrayList<>();

        try (PreparedStatement ps = Database.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(new AppUser(rs.getLong("user_id"), rs.getString("display_name")));
            }
            return users;

        } catch (SQLException e) {
            throw new DataAccessException("프로필 목록을 불러오지 못했습니다.", e);
        }
    }

    /**
     * 새 프로필을 만든다.
     *
     * @throws DataAccessException 같은 이름이 이미 있는 경우 (display_name UNIQUE)
     */
    public static AppUser insert(String displayName) {
        String sql = "INSERT INTO app_user (display_name, created_at) VALUES (?, ?)";

        try (PreparedStatement ps = Database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, displayName);
            ps.setString(2, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : AppUser.UNSAVED;
                return new AppUser(id, displayName);
            }

        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new DataAccessException("'" + displayName + "' 프로필이 이미 있습니다.", e);
            }
            throw new DataAccessException("프로필을 만들지 못했습니다.", e);
        }
    }

    /** 현재 선택된 프로필. 아직 없으면 비어 있다. */
    public static Optional<AppUser> currentUser() {
        String raw = getSetting(CURRENT_USER_KEY);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return findById(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * 현재 프로필의 id. 호출 전에 {@link #ensureDefaultUser()} 가 실행돼 있어야 한다.
     */
    public static long currentUserId() {
        return currentUser()
                .orElseThrow(() -> new IllegalStateException(
                        "현재 사용자가 없습니다. UserRepository.ensureDefaultUser() 를 먼저 호출하세요."))
                .id();
    }

    /** 현재 프로필을 바꾼다. */
    public static void setCurrentUser(long userId) {
        putSetting(CURRENT_USER_KEY, String.valueOf(userId));
    }

    public static Optional<AppUser> findById(long userId) {
        String sql = "SELECT user_id, display_name FROM app_user WHERE user_id = ?";

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AppUser(rs.getLong("user_id"), rs.getString("display_name")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("프로필을 불러오지 못했습니다.", e);
        }
    }

    // ----- app_setting 키-값 -----

    private static String getSetting(String key) {
        String sql = "SELECT setting_value FROM app_setting WHERE setting_key = ?";

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("설정값을 읽지 못했습니다: " + key, e);
        }
    }

    private static void putSetting(String key, String value) {
        // 있으면 덮어쓰고 없으면 넣는다 (SQLite UPSERT)
        String sql = """
                INSERT INTO app_setting (setting_key, setting_value) VALUES (?, ?)
                ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                """;

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("설정값을 저장하지 못했습니다: " + key, e);
        }
    }

    static boolean isUniqueViolation(SQLException e) {
        String message = e.getMessage();
        return message != null && message.contains("UNIQUE constraint failed");
    }

    /** 패키지 밖에서도 연결을 직접 쓸 일이 있을 때를 위한 통로 */
    static Connection connection() {
        return Database.connection();
    }
}

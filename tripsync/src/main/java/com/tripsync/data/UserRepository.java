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

public final class UserRepository {

    private static final String DEFAULT_USER_NAME = "나";

    private static final String CURRENT_USER_KEY = "currentUserId";

    private UserRepository() {
    }

    public static void ensureDefaultUser() {
        if (findAll().isEmpty()) {
            AppUser created = insert(DEFAULT_USER_NAME);
            setCurrentUser(created.id());
            return;
        }

        if (currentUser().isEmpty()) {
            setCurrentUser(findAll().get(0).id());
        }
    }

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

    public static long currentUserId() {
        return currentUser()
                .orElseThrow(() -> new IllegalStateException(
                        "현재 사용자가 없습니다. UserRepository.ensureDefaultUser() 를 먼저 호출하세요."))
                .id();
    }

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

    static Connection connection() {
        return Database.connection();
    }
}

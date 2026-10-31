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

import com.tripsync.model.Group;
import com.tripsync.util.InviteCode;

public final class GroupRepository {

    private static final int CODE_RETRY_LIMIT = 20;

    private GroupRepository() {
    }

    public static Group create(String name, long creatorUserId) {
        Connection conn = Database.connection();
        String inviteCode = generateUniqueInviteCode();
        LocalDateTime now = LocalDateTime.now();

        String insertGroup =
                "INSERT INTO travel_group (group_name, invite_code, created_at) VALUES (?, ?, ?)";
        String insertMember =
                "INSERT INTO group_member (group_id, user_id, joined_at) VALUES (?, ?, ?)";

        try {
            conn.setAutoCommit(false);

            long groupId;
            try (PreparedStatement ps = conn.prepareStatement(insertGroup, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, inviteCode);
                ps.setString(3, now.toString());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new DataAccessException("모임 번호를 발급받지 못했습니다.");
                    }
                    groupId = keys.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertMember)) {
                ps.setLong(1, groupId);
                ps.setLong(2, creatorUserId);
                ps.setString(3, now.toString());
                ps.executeUpdate();
            }

            conn.commit();
            return new Group(groupId, name, inviteCode, now, 1);

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("모임을 저장하지 못했습니다.", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    public static List<Group> findAllForUser(long userId) {
        String sql = """
                SELECT  g.group_id,
                        g.group_name,
                        g.invite_code,
                        g.created_at,
                        (SELECT COUNT(*) FROM group_member m2 WHERE m2.group_id = g.group_id) AS member_count
                FROM    travel_group g
                JOIN    group_member m ON m.group_id = g.group_id
                WHERE   m.user_id = ?
                ORDER BY m.joined_at DESC
                """;

        List<Group> groups = new ArrayList<>();

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapRow(rs));
                }
            }
            return groups;

        } catch (SQLException e) {
            throw new DataAccessException("모임 목록을 불러오지 못했습니다.", e);
        }
    }

    public static Optional<Group> findById(long groupId) {
        String sql = """
                SELECT  g.group_id, g.group_name, g.invite_code, g.created_at,
                        (SELECT COUNT(*) FROM group_member m WHERE m.group_id = g.group_id) AS member_count
                FROM    travel_group g
                WHERE   g.group_id = ?
                """;
        return queryOne(sql, ps -> ps.setLong(1, groupId), "모임을 불러오지 못했습니다.");
    }

    public static Optional<Group> findByInviteCode(String rawCode) {
        String code = InviteCode.normalize(rawCode);
        String sql = """
                SELECT  g.group_id, g.group_name, g.invite_code, g.created_at,
                        (SELECT COUNT(*) FROM group_member m WHERE m.group_id = g.group_id) AS member_count
                FROM    travel_group g
                WHERE   g.invite_code = ?
                """;
        return queryOne(sql, ps -> ps.setString(1, code), "초대 코드를 조회하지 못했습니다.");
    }

    public static boolean isMember(long groupId, long userId) {
        String sql = "SELECT 1 FROM group_member WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("참여 여부를 확인하지 못했습니다.", e);
        }
    }

    public static boolean join(long groupId, long userId) {
        if (isMember(groupId, userId)) {
            return false;
        }
        String sql = "INSERT INTO group_member (group_id, user_id, joined_at) VALUES (?, ?, ?)";

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, userId);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            throw new DataAccessException("모임에 참여하지 못했습니다.", e);
        }
    }

    public static List<String> findMemberNames(long groupId) {
        String sql = """
                SELECT  u.display_name
                FROM    group_member m
                JOIN    app_user u ON u.user_id = m.user_id
                WHERE   m.group_id = ?
                ORDER BY m.joined_at
                """;

        List<String> names = new ArrayList<>();

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
            return names;

        } catch (SQLException e) {
            throw new DataAccessException("참여자 목록을 불러오지 못했습니다.", e);
        }
    }

    private static String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < CODE_RETRY_LIMIT; attempt++) {
            String candidate = InviteCode.generate();
            if (findByInviteCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new DataAccessException("사용 가능한 초대 코드를 찾지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static Optional<Group> queryOne(String sql, Binder binder, String errorMessage) {
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    private static Group mapRow(ResultSet rs) throws SQLException {
        return new Group(
                rs.getLong("group_id"),
                rs.getString("group_name"),
                rs.getString("invite_code"),
                LocalDateTime.parse(rs.getString("created_at")),
                rs.getInt("member_count"));
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {

        }
    }

    private static void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {

        }
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}

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

/**
 * 모임 저장소.
 *
 * <p>모임 생성과 초대 코드 참여가 모두 여기를 거친다.
 * SQL 은 모두 {@link PreparedStatement} 로 값을 바인딩한다. 문자열을 이어 붙여
 * 쿼리를 만들면 모임 이름에 따옴표가 들어오는 순간 깨지고, SQL 삽입에도 노출된다.</p>
 */
public final class GroupRepository {

    /** 초대 코드가 겹쳤을 때 다시 뽑아보는 최대 횟수 */
    private static final int CODE_RETRY_LIMIT = 20;

    private GroupRepository() {
    }

    /**
     * 모임을 만들고, 만든 사람을 첫 참여자로 넣는다.
     *
     * <p>모임 삽입과 참여자 삽입은 한 트랜잭션으로 묶는다. 둘 중 하나만 성공하면
     * '참여자가 아무도 없는 모임'이 남아 목록에 보이지도 않고 지울 수도 없게 된다.</p>
     *
     * @param name          모임 이름
     * @param creatorUserId 만든 사람의 프로필 id
     * @return 저장된 모임 (id 와 초대 코드가 채워져 있다)
     */
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

    /**
     * 주어진 사용자가 참여 중인 모임을 최근 참여 순으로 돌려준다.
     *
     * <p>참여자 수는 {@code COUNT} 서브쿼리로 한 번에 가져온다. 모임마다 따로 세면
     * 목록 길이만큼 쿼리가 늘어난다.</p>
     */
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

    /** 초대 코드로 모임을 찾는다. 대소문자와 공백은 무시한다. */
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

    /** 이미 참여 중인지 확인한다. */
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

    /**
     * 모임에 참여시킨다.
     *
     * @return 새로 참여했으면 {@code true}, 이미 참여 중이었으면 {@code false}
     */
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

    /** 모임 참여자들의 표시 이름을 참여한 순서대로 돌려준다. */
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

    /**
     * 아직 쓰이지 않은 초대 코드를 만든다.
     *
     * <p>코드가 짧아 아주 낮은 확률로 겹칠 수 있다. 겹치면 다시 뽑고,
     * 여러 번 실패하면 예외를 던져 조용히 틀린 코드가 저장되는 일을 막는다.</p>
     */
    private static String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < CODE_RETRY_LIMIT; attempt++) {
            String candidate = InviteCode.generate();
            if (findByInviteCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new DataAccessException("사용 가능한 초대 코드를 찾지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    // ----- 공통 도우미 -----

    /** 한 건 조회용. 바인딩 부분만 호출한 쪽에서 채운다. */
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
            // 롤백 실패까지 사용자에게 알릴 필요는 없다. 원래 예외가 더 중요하다.
        }
    }

    private static void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
            // 같은 이유
        }
    }

    /** 람다로 PreparedStatement 바인딩을 넘기기 위한 함수형 인터페이스 */
    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}

package com.tripsync.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tripsync.model.Trip;

/**
 * 여행 저장소.
 *
 * <p>날짜는 {@code LocalDate.toString()} 의 ISO 형식({@code 2026-11-14})으로 저장한다.
 * ISO 형식은 문자열로 정렬해도 시간순이 되므로 {@code ORDER BY start_date} 가
 * 별도 변환 없이 그대로 동작한다.</p>
 */
public final class TripRepository {

    private TripRepository() {
    }

    /**
     * 여행을 만든다.
     *
     * <p>종료일이 시작일보다 빠른지는 화면에서 이미 검사하지만, 저장소에서도 한 번 더 막는다.
     * 화면을 거치지 않고 호출되는 경로가 생겨도 잘못된 데이터가 들어가지 않게 하기 위해서다.</p>
     */
    public static Trip create(long groupId, String title, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new DataAccessException("종료일은 시작일보다 빠를 수 없습니다.");
        }

        String sql = """
                INSERT INTO trip (group_id, title, start_date, end_date, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement ps = Database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, groupId);
            ps.setString(2, title);
            ps.setString(3, startDate.toString());
            ps.setString(4, endDate.toString());
            ps.setString(5, now.toString());
            ps.setString(6, now.toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("여행 번호를 발급받지 못했습니다.");
                }
                return new Trip(keys.getLong(1), groupId, title, startDate, endDate, now, 0);
            }

        } catch (SQLException e) {
            throw new DataAccessException("여행을 저장하지 못했습니다.", e);
        }
    }

    /**
     * 한 모임의 여행을 시작일이 빠른 순으로 돌려준다.
     *
     * <p>일정 개수는 서브쿼리로 함께 가져온다. 여행마다 따로 세면
     * 목록 길이만큼 쿼리가 늘어난다.</p>
     */
    public static List<Trip> findAllByGroup(long groupId) {
        String sql = """
                SELECT  t.trip_id, t.group_id, t.title, t.start_date, t.end_date, t.updated_at,
                        (SELECT COUNT(*) FROM schedule_item s WHERE s.trip_id = t.trip_id) AS schedule_count
                FROM    trip t
                WHERE   t.group_id = ?
                ORDER BY t.start_date, t.trip_id
                """;

        List<Trip> trips = new ArrayList<>();

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(mapRow(rs));
                }
            }
            return trips;

        } catch (SQLException e) {
            throw new DataAccessException("여행 목록을 불러오지 못했습니다.", e);
        }
    }

    public static Optional<Trip> findById(long tripId) {
        String sql = """
                SELECT  t.trip_id, t.group_id, t.title, t.start_date, t.end_date, t.updated_at,
                        (SELECT COUNT(*) FROM schedule_item s WHERE s.trip_id = t.trip_id) AS schedule_count
                FROM    trip t
                WHERE   t.trip_id = ?
                """;

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("여행을 불러오지 못했습니다.", e);
        }
    }

    /**
     * 여행의 이름과 기간을 수정한다.
     *
     * <p>일정이 이미 들어 있는데 기간을 줄이면 기간 밖에 남는 일정이 생긴다.
     * 일정을 말없이 지우면 사용자가 적어둔 내용이 사라지므로, 여기서는 지우지 않고
     * 여행 상세 화면이 "기간 밖 일정"으로 따로 보여준다. [4단계]</p>
     */
    public static void update(long tripId, String title, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new DataAccessException("종료일은 시작일보다 빠를 수 없습니다.");
        }

        String sql = "UPDATE trip SET title = ?, start_date = ?, end_date = ?, updated_at = ? WHERE trip_id = ?";

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
            ps.setString(4, LocalDateTime.now().toString());
            ps.setLong(5, tripId);

            if (ps.executeUpdate() == 0) {
                throw new DataAccessException("수정할 여행을 찾지 못했습니다.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("여행을 수정하지 못했습니다.", e);
        }
    }

    /**
     * 여행을 지운다. 스키마의 ON DELETE CASCADE 에 따라
     * 그 여행의 일정과 위시리스트도 함께 지워진다.
     */
    public static void delete(long tripId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM trip WHERE trip_id = ?")) {
            ps.setLong(1, tripId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("여행을 삭제하지 못했습니다.", e);
        }
    }

    /** 여행에 내용이 들어 있는지 확인한다. 삭제 전 경고 문구를 고를 때 쓴다. */
    public static boolean hasContent(long tripId) {
        String sql = """
                SELECT (SELECT COUNT(*) FROM schedule_item WHERE trip_id = ?)
                     + (SELECT COUNT(*) FROM wishlist_item WHERE trip_id = ?)
                """;

        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setLong(1, tripId);
            ps.setLong(2, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("여행 내용을 확인하지 못했습니다.", e);
        }
    }

    private static Trip mapRow(ResultSet rs) throws SQLException {
        return new Trip(
                rs.getLong("trip_id"),
                rs.getLong("group_id"),
                rs.getString("title"),
                LocalDate.parse(rs.getString("start_date")),
                LocalDate.parse(rs.getString("end_date")),
                LocalDateTime.parse(rs.getString("updated_at")),
                rs.getInt("schedule_count"));
    }
}

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

public final class TripRepository {

    private TripRepository() {
    }

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

    public static void delete(long tripId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM trip WHERE trip_id = ?")) {
            ps.setLong(1, tripId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("여행을 삭제하지 못했습니다.", e);
        }
    }

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

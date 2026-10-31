-- =========================================================================
--  TripSync 로컬 데이터베이스 스키마 (SQLite)
--  -----------------------------------------------------------------------
--  앱이 켜질 때마다 이 파일을 실행한다. 모든 문장에 IF NOT EXISTS 가 붙어
--  있어 여러 번 실행해도 안전하다(멱등).
--
--  날짜/시간은 SQLite 에 전용 타입이 없으므로 ISO-8601 문자열로 저장한다.
--    날짜      '2026-11-14'
--    시간      '14:30'
--    생성시각  '2026-10-31T16:03:11'
--  ISO 형식은 문자열로 정렬해도 시간순이 되므로 ORDER BY 가 그대로 동작한다.
-- =========================================================================

-- 외래 키 제약을 실제로 적용한다. SQLite 는 기본값이 OFF 라 연결마다 켜야 한다.
PRAGMA foreign_keys = ON;


-- -------------------------------------------------------------------------
--  사용자 (로컬 프로필)
--
--  이 과제는 로그인 서버가 범위 밖이라, 설계서 9장의 허용대로 userId 대신
--  '표시 이름' 수준으로 단순화했다. 한 PC 안에서 프로필을 바꿔가며
--  초대 코드 참여 흐름을 시연하기 위한 장치다.
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
    user_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    display_name TEXT    NOT NULL UNIQUE,
    created_at   TEXT    NOT NULL
);


-- -------------------------------------------------------------------------
--  앱 설정 (키-값)
--  현재 선택된 프로필 등 한 줄짜리 상태를 담는다.
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_setting (
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);


-- -------------------------------------------------------------------------
--  모임
--  'group' 은 SQL 예약어라 테이블 이름을 travel_group 으로 둔다.
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS travel_group (
    group_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    group_name  TEXT    NOT NULL,
    invite_code TEXT    NOT NULL UNIQUE,
    created_at  TEXT    NOT NULL
);


-- -------------------------------------------------------------------------
--  모임 참여자
--  (모임, 사용자) 쌍이 기본 키라 같은 사람이 같은 모임에 두 번 들어갈 수 없다.
--  중복 참여 방지를 애플리케이션 코드가 아니라 DB 제약으로 보장한다.
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_member (
    group_id  INTEGER NOT NULL REFERENCES travel_group(group_id) ON DELETE CASCADE,
    user_id   INTEGER NOT NULL REFERENCES app_user(user_id)      ON DELETE CASCADE,
    joined_at TEXT    NOT NULL,
    PRIMARY KEY (group_id, user_id)
);


-- -------------------------------------------------------------------------
--  여행  [3단계에서 사용]
--  모임이 지워지면 그 안의 여행도 함께 지워진다(ON DELETE CASCADE).
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trip (
    trip_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id   INTEGER NOT NULL REFERENCES travel_group(group_id) ON DELETE CASCADE,
    title      TEXT    NOT NULL,
    start_date TEXT    NOT NULL,
    end_date   TEXT    NOT NULL,
    created_at TEXT    NOT NULL,
    updated_at TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_trip_group ON trip(group_id);


-- -------------------------------------------------------------------------
--  일정  [4단계에서 사용]
--  start_time 과 memo 는 선택 입력이라 NULL 을 허용한다.
--  장소는 설계서대로 텍스트로만 관리한다(지도 검색 없음).
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule_item (
    schedule_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id      INTEGER NOT NULL REFERENCES trip(trip_id) ON DELETE CASCADE,
    visit_date   TEXT    NOT NULL,
    start_time   TEXT,
    place_name   TEXT    NOT NULL,
    category     TEXT,
    memo         TEXT,
    created_at   TEXT    NOT NULL
);

-- 여행 상세 화면은 '특정 여행의 특정 날짜' 일정을 계속 조회하므로 함께 묶어 색인한다.
CREATE INDEX IF NOT EXISTS idx_schedule_trip_date ON schedule_item(trip_id, visit_date);


-- -------------------------------------------------------------------------
--  공동 위시리스트  [5단계에서 사용]
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlist_item (
    item_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id    INTEGER NOT NULL REFERENCES trip(trip_id) ON DELETE CASCADE,
    place_name TEXT    NOT NULL,
    note       TEXT,
    category   TEXT,
    created_at TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wishlist_trip ON wishlist_item(trip_id);

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS app_user (
    user_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    display_name TEXT    NOT NULL UNIQUE,
    created_at   TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS app_setting (
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);

CREATE TABLE IF NOT EXISTS travel_group (
    group_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    group_name  TEXT    NOT NULL,
    invite_code TEXT    NOT NULL UNIQUE,
    created_at  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS group_member (
    group_id  INTEGER NOT NULL REFERENCES travel_group(group_id) ON DELETE CASCADE,
    user_id   INTEGER NOT NULL REFERENCES app_user(user_id)      ON DELETE CASCADE,
    joined_at TEXT    NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

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

CREATE INDEX IF NOT EXISTS idx_schedule_trip_date ON schedule_item(trip_id, visit_date);

CREATE TABLE IF NOT EXISTS wishlist_item (
    item_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id    INTEGER NOT NULL REFERENCES trip(trip_id) ON DELETE CASCADE,
    place_name TEXT    NOT NULL,
    note       TEXT,
    category   TEXT,
    created_at TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wishlist_trip ON wishlist_item(trip_id);

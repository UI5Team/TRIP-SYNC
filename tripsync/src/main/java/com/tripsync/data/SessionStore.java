package com.tripsync.data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.tripsync.model.Category;
import com.tripsync.model.ScheduleItem;
import com.tripsync.model.WishlistItem;

public final class SessionStore {

    private static final Map<Long, List<ScheduleItem>> SCHEDULES = new HashMap<>();
    private static final Map<Long, List<WishlistItem>> WISHES = new HashMap<>();

    private static long sequence = 1;

    private SessionStore() {
    }

    private static long nextId() {
        return sequence++;
    }

    private static final Comparator<ScheduleItem> BY_TIME = Comparator
            .comparing(ScheduleItem::date)
            .thenComparing(item -> item.time() == null ? LocalTime.MAX : item.time())
            .thenComparing(ScheduleItem::id);

    public static List<ScheduleItem> schedulesOf(long tripId) {
        List<ScheduleItem> items = new ArrayList<>(SCHEDULES.getOrDefault(tripId, List.of()));
        items.sort(BY_TIME);
        return items;
    }

    public static List<ScheduleItem> schedulesOn(long tripId, LocalDate date) {
        return schedulesOf(tripId).stream()
                .filter(item -> item.date().equals(date))
                .toList();
    }

    public static Set<LocalDate> scheduledDates(long tripId) {
        Set<LocalDate> dates = new HashSet<>();
        for (ScheduleItem item : SCHEDULES.getOrDefault(tripId, List.of())) {
            dates.add(item.date());
        }
        return dates;
    }

    public static Optional<ScheduleItem> scheduleById(long tripId, long scheduleId) {
        return schedulesOf(tripId).stream()
                .filter(item -> item.id() == scheduleId)
                .findFirst();
    }

    public static ScheduleItem addSchedule(ScheduleItem item) {
        ScheduleItem saved = item.withId(nextId());
        SCHEDULES.computeIfAbsent(saved.tripId(), key -> new ArrayList<>()).add(saved);
        return saved;
    }

    public static void updateSchedule(ScheduleItem item) {
        List<ScheduleItem> items = SCHEDULES.get(item.tripId());
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id() == item.id()) {
                items.set(i, item);
                return;
            }
        }
    }

    public static void deleteSchedule(long tripId, long scheduleId) {
        List<ScheduleItem> items = SCHEDULES.get(tripId);
        if (items != null) {
            items.removeIf(item -> item.id() == scheduleId);
        }
    }

    public static List<WishlistItem> wishesOf(long tripId) {
        return new ArrayList<>(WISHES.getOrDefault(tripId, List.of()));
    }

    public static WishlistItem addWish(long tripId, String placeName, Category category, String note) {
        WishlistItem saved = new WishlistItem(nextId(), tripId, placeName, category, note);
        WISHES.computeIfAbsent(tripId, key -> new ArrayList<>()).add(saved);
        return saved;
    }

    public static void deleteWish(long tripId, long itemId) {
        List<WishlistItem> items = WISHES.get(tripId);
        if (items != null) {
            items.removeIf(item -> item.id() == itemId);
        }
    }

    public static boolean hasWish(long tripId, String placeName) {
        return wishesOf(tripId).stream()
                .anyMatch(item -> item.placeName().equalsIgnoreCase(placeName));
    }

    public static void fillSampleDay(long tripId, LocalDate date) {
        add(tripId, date, LocalTime.of(9, 0), "인천공항 출발", Category.MOVE, "인천국제공항 제1터미널");
        add(tripId, date, LocalTime.of(11, 0), "숙소 체크인", Category.STAY, "오사카 호텔");
        add(tripId, date, LocalTime.of(13, 0), "점심 식사", Category.FOOD, "도톤보리");
        add(tripId, date, LocalTime.of(15, 0), "오사카성 관광", Category.SIGHT, "천수각 전망대 포함");
        add(tripId, date, LocalTime.of(19, 0), "저녁 식사", Category.FOOD, "우메다 맛집");
    }

    public static void fillSampleWishes(long tripId) {
        wish(tripId, "오사카성", Category.SIGHT, "천수각 전망대");
        wish(tripId, "도톤보리", Category.FOOD, "글리코 간판 앞");
        wish(tripId, "우메다 스카이빌딩", Category.SIGHT, "야경 추천");
        wish(tripId, "난바 쇼핑거리", Category.SHOP, "기념품");
        wish(tripId, "신사이바시 카페", Category.CAFE, "아침 일찍 오픈");
    }

    private static void wish(long tripId, String placeName, Category category, String note) {
        if (!hasWish(tripId, placeName)) {
            addWish(tripId, placeName, category, note);
        }
    }

    private static void add(long tripId, LocalDate date, LocalTime time,
                            String placeName, Category category, String memo) {
        addSchedule(new ScheduleItem(ScheduleItem.UNSAVED, tripId, date, time, placeName, category, memo));
    }

    public static void clear() {
        SCHEDULES.clear();
        WISHES.clear();
    }
}

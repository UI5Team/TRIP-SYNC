package com.tripsync.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleItem(long id,
                           long tripId,
                           LocalDate date,
                           LocalTime time,
                           String placeName,
                           Category category,
                           String memo) {

    public static final long UNSAVED = -1L;

    public boolean hasTime() {
        return time != null;
    }

    public boolean hasMemo() {
        return memo != null && !memo.isBlank();
    }

    public ScheduleItem withId(long newId) {
        return new ScheduleItem(newId, tripId, date, time, placeName, category, memo);
    }
}

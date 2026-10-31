package com.tripsync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Trip(long id,
                   long groupId,
                   String title,
                   LocalDate startDate,
                   LocalDate endDate,
                   LocalDateTime updatedAt,
                   int scheduleCount) {

    public static final long UNSAVED = -1L;

    public boolean isSaved() {
        return id != UNSAVED;
    }

    public int dayCount() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public LocalDate dateAt(int dayIndex) {
        return startDate.plusDays(dayIndex);
    }
}

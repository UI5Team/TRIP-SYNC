package com.tripsync.nav;

import java.util.Optional;

public final class AppContext {

    private static Long currentGroupId;
    private static String currentGroupName;

    private static Long currentTripId;
    private static String currentTripTitle;

    private AppContext() {
    }

    public static void setCurrentGroup(long groupId, String groupName) {
        currentGroupId = groupId;
        currentGroupName = groupName;

        currentTripId = null;
        currentTripTitle = null;
    }

    public static void setCurrentTrip(long tripId, String title) {
        currentTripId = tripId;
        currentTripTitle = title;
    }

    public static Optional<Long> currentGroupId() {
        return Optional.ofNullable(currentGroupId);
    }

    public static Optional<String> currentGroupName() {
        return Optional.ofNullable(currentGroupName);
    }

    public static Optional<Long> currentTripId() {
        return Optional.ofNullable(currentTripId);
    }

    public static Optional<String> currentTripTitle() {
        return Optional.ofNullable(currentTripTitle);
    }

    public static void clear() {
        currentGroupId = null;
        currentGroupName = null;
        currentTripId = null;
        currentTripTitle = null;
    }
}

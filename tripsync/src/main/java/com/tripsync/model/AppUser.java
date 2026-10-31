package com.tripsync.model;

public record AppUser(long id, String displayName) {

    public static final long UNSAVED = -1L;

    public static AppUser of(String displayName) {
        return new AppUser(UNSAVED, displayName);
    }

    public boolean isSaved() {
        return id != UNSAVED;
    }
}

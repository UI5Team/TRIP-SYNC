package com.tripsync.model;

import java.time.LocalDateTime;

public record Group(long id,
                    String name,
                    String inviteCode,
                    LocalDateTime createdAt,
                    int memberCount) {

    public static final long UNSAVED = -1L;

    public static Group of(String name, String inviteCode) {
        return new Group(UNSAVED, name, inviteCode, LocalDateTime.now(), 0);
    }

    public boolean isSaved() {
        return id != UNSAVED;
    }
}

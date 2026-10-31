package com.tripsync.model;

public record WishlistItem(long id,
                           long tripId,
                           String placeName,
                           Category category,
                           String note) {

    public static final long UNSAVED = -1L;

    public boolean hasNote() {
        return note != null && !note.isBlank();
    }
}

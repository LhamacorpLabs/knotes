package com.lhamacorp.knotes.api.dto;

/**
 * Update payload. Any field left out (null) is not changed, except {@code content}:
 * when {@code content} is null and {@code color} or {@code pinned} is present, the request is
 * treated as a display-only update and the note body is left untouched.
 * An empty {@code color} clears the color.
 */
public record NoteUpdateRequest(String content, String encryptionMode, String color, Boolean pinned) {

    public NoteUpdateRequest(String content, String encryptionMode) {
        this(content, encryptionMode, null, null);
    }
}

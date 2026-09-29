package com.lhamacorp.knotes.api.dto;

import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;

import java.time.Instant;

public record NoteResponse(
        String id,
        String content,
        String createdBy,
        Instant createdAt,
        Instant modifiedAt,
        EncryptionMode encryptionMode,
        Boolean requiresPassword,
        String color,
        Boolean pinned
) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.id(),
                note.content(),
                note.createdBy(),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false,
                note.color(),
                Boolean.TRUE.equals(note.pinned())
        );
    }

    public static NoteResponse fromPrivate(Note note, String requestingUserId) {
        return new NoteResponse(
                note.id(),
                note.content(requestingUserId, null),
                note.createdBy(),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false,
                note.color(),
                Boolean.TRUE.equals(note.pinned())
        );
    }

    public static NoteResponse fromPasswordShared(Note note, String password) {
        return new NoteResponse(
                note.id(),
                note.content(null, password),
                note.createdBy(),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false,
                note.color(),
                Boolean.TRUE.equals(note.pinned())
        );
    }

    public static NoteResponse fromWithAuth(Note note, String requestingUserId, String password) {
        return new NoteResponse(
                note.id(),
                note.content(requestingUserId, password),
                note.createdBy(),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false,
                note.color(),
                Boolean.TRUE.equals(note.pinned())
        );
    }

    /**
     * Response for list views: never throws on undecryptable notes; content is null instead.
     */
    public static NoteResponse summary(Note note, String requestingUserId) {
        String content;
        try {
            content = note.content(requestingUserId, null);
        } catch (RuntimeException e) {
            content = null;
        }
        return new NoteResponse(
                note.id(),
                content,
                note.createdBy(),
                note.createdAt(),
                note.modifiedAt(),
                note.encryptionMode() != null ? note.encryptionMode() : EncryptionMode.PUBLIC,
                note.requiresPassword() != null ? note.requiresPassword() : false,
                note.color(),
                Boolean.TRUE.equals(note.pinned())
        );
    }
}

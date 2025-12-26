package com.lhamacorp.knotes.api.dto;

import com.lhamacorp.knotes.domain.Note;

import java.time.Instant;

public record NoteResponse(String id, String content, Instant createdAt, Instant modifiedAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(
            note.id(),
            note.content(),
            note.createdAt(),
            note.modifiedAt()
        );
    }
}
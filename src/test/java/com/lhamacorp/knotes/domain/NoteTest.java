package com.lhamacorp.knotes.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class NoteTest {

    @Test
    void constructor_withStringContent_shouldCompressAndStore() {
        // Given
        String id = "test-id";
        String content = "This is a test note with some content that should be compressed.";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, content, now, now);

        // Then
        assertEquals(id, note.id());
        assertEquals(content, note.content()); // Should decompress correctly
        assertEquals(now, note.createdAt());
        assertEquals(now, note.modifiedAt());

        // Verify that content is actually stored compressed
        assertNotNull(note.compressedContent());
        assertTrue(note.compressedContent().getData().length > 0);
    }

    @Test
    void constructor_withNullContent_shouldHandleGracefully() {
        // Given
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, (String) null, now, now);

        // Then
        assertEquals(id, note.id());
        assertNull(note.content());
        assertNull(note.compressedContent());
    }

    @Test
    void content_withCompressedContent_shouldDecompressCorrectly() {
        // Given
        String originalContent = "Test note content for compression verification.";
        String id = "test-id";
        Instant now = Instant.now();

        // When
        Note note = new Note(id, originalContent, now, now);
        String retrievedContent = note.content();

        // Then
        assertEquals(originalContent, retrievedContent);
    }
}
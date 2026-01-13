package com.lhamacorp.knotes.service;

import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.exception.NotFoundException;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository repository;

    @InjectMocks
    private NoteService noteService;

    private Note testNote;
    private String testId;
    private String testContent;
    private Instant testCreatedAt;
    private Instant testModifiedAt;

    @BeforeEach
    void setUp() {
        testId = "01ABCDEF1234567890ABCDEF12";
        testContent = "This is a test note content";
        testCreatedAt = Instant.parse("2024-01-01T10:00:00Z");
        testModifiedAt = Instant.parse("2024-01-01T11:00:00Z");
        testNote = new Note(testId, testContent, testCreatedAt, testModifiedAt);
    }

    @Test
    void exists_whenNoteExists_shouldReturnTrue() {
        // Given
        when(repository.existsById(testId)).thenReturn(true);

        // When
        boolean result = noteService.exists(testId);

        // Then
        assertTrue(result);
        verify(repository).existsById(testId);
    }

    @Test
    void exists_whenNoteDoesNotExist_shouldReturnFalse() {
        // Given
        when(repository.existsById(testId)).thenReturn(false);

        // When
        boolean result = noteService.exists(testId);

        // Then
        assertFalse(result);
        verify(repository).existsById(testId);
    }

    @Test
    void findById_whenNoteExists_shouldReturnNote() {
        // Given
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));

        // When
        Note result = noteService.findById(testId);

        // Then
        assertEquals(testNote, result);
        assertEquals(testId, result.id());
        assertEquals(testContent, result.content());
        assertEquals(testCreatedAt, result.createdAt());
        assertEquals(testModifiedAt, result.modifiedAt());
        verify(repository).findById(testId);
    }

    @Test
    void findById_whenNoteDoesNotExist_shouldThrowNotFoundException() {
        // Given
        when(repository.findById(testId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> noteService.findById(testId));

        assertEquals("Note not found!", exception.getMessage());
        verify(repository).findById(testId);
    }

    @Test
    void findMetadataById_whenNoteExists_shouldReturnMetadata() {
        // Given
        when(repository.findMetadataById(testId)).thenReturn(Optional.of(testNote));

        // When
        NoteMetadata result = noteService.findMetadataById(testId);

        // Then
        assertNotNull(result);
        assertEquals(testId, result.id());
        assertEquals(testCreatedAt, result.createdAt());
        assertEquals(testModifiedAt, result.modifiedAt());
        verify(repository).findMetadataById(testId);
    }

    @Test
    void findMetadataById_whenNoteDoesNotExist_shouldThrowNotFoundException() {
        // Given
        when(repository.findMetadataById(testId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> noteService.findMetadataById(testId));

        assertEquals("Note not found!", exception.getMessage());
        verify(repository).findMetadataById(testId);
    }

    @Test
    void save_shouldCreateNewNoteWithGeneratedId() {
        // Given
        String content = "New note content";
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        Note savedNote = new Note("generated-ulid", content, Instant.now(), Instant.now());
        when(repository.save(any(Note.class))).thenReturn(savedNote);

        // When
        Note result = noteService.save(content);

        // Then
        assertEquals(savedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertNotNull(capturedNote.id());
        assertEquals(content, capturedNote.content());
        assertNotNull(capturedNote.createdAt());
        assertNotNull(capturedNote.modifiedAt());
        assertEquals(capturedNote.createdAt(), capturedNote.modifiedAt());
    }

    @Test
    void save_withNullContent_shouldHandleGracefully() {
        // Given
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        Note savedNote = new Note("generated-ulid", (String) null, Instant.now(), Instant.now());
        when(repository.save(any(Note.class))).thenReturn(savedNote);

        // When
        Note result = noteService.save(null);

        // Then
        assertEquals(savedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertNull(capturedNote.content());
    }

    @Test
    void save_withEmptyContent_shouldCreateNote() {
        // Given
        String emptyContent = "";
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        Note savedNote = new Note("generated-ulid", emptyContent, Instant.now(), Instant.now());
        when(repository.save(any(Note.class))).thenReturn(savedNote);

        // When
        Note result = noteService.save(emptyContent);

        // Then
        assertEquals(savedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertEquals(emptyContent, capturedNote.content());
    }

    @Test
    void update_whenNoteExists_shouldUpdateContentAndModifiedDate() {
        // Given
        String updatedContent = "Updated note content";
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        Note updatedNote = new Note(testId, updatedContent, testCreatedAt, Instant.now());
        when(repository.save(any(Note.class))).thenReturn(updatedNote);

        // When
        Note result = noteService.update(testId, updatedContent);

        // Then
        assertEquals(updatedNote, result);
        verify(repository).findById(testId);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertEquals(testId, capturedNote.id());
        assertEquals(updatedContent, capturedNote.content());
        assertEquals(testCreatedAt, capturedNote.createdAt());
        assertTrue(capturedNote.modifiedAt().isAfter(testCreatedAt));
    }

    @Test
    void update_whenNoteDoesNotExist_shouldThrowNotFoundException() {
        // Given
        String updatedContent = "Updated content";
        when(repository.findById(testId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> noteService.update(testId, updatedContent));

        assertEquals("Note not found!", exception.getMessage());
        verify(repository).findById(testId);
        verify(repository, never()).save(any(Note.class));
    }

    @Test
    void update_withNullContent_shouldUpdateToNull() {
        // Given
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        Note updatedNote = new Note(testId, (String) null, testCreatedAt, Instant.now());
        when(repository.save(any(Note.class))).thenReturn(updatedNote);

        // When
        Note result = noteService.update(testId, null);

        // Then
        assertEquals(updatedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertNull(capturedNote.content());
    }
}
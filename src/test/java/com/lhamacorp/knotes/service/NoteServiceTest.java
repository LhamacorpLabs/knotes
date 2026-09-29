package com.lhamacorp.knotes.service;

import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.exception.BadRequestException;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
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
    private UserContext testUserContext;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testId = "01ABCDEF1234567890ABCDEF12";
        testUserId = "user123";
        testContent = "This is a test content content";
        testCreatedAt = Instant.parse("2024-01-01T10:00:00Z");
        testModifiedAt = Instant.parse("2024-01-01T11:00:00Z");
        testNote = new Note(testId, testContent, testUserId, testCreatedAt, testModifiedAt);
        testUserContext = new UserContext(testUserId, "testuser", List.of("USER"));
        UserContextHolder.set(testUserContext);
    }

    @AfterEach
    void tearDown() {
        // Clean up UserContextHolder to avoid test pollution
        UserContextHolder.clear();
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
        BadRequestException exception = assertThrows(BadRequestException.class,
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
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> noteService.findMetadataById(testId));

        assertEquals("Note not found!", exception.getMessage());
        verify(repository).findMetadataById(testId);
    }

    @Test
    void save_shouldCreateNewNoteWithGeneratedId() {
        // Given
        String content = "New content content";
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        Note savedNote = new Note("generated-ulid", content, testUserId, now(), now());
        when(repository.save(any(Note.class))).thenReturn(savedNote);

        // When
        Note result = noteService.save(content, EncryptionMode.PUBLIC);

        // Then
        assertEquals(savedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertNotNull(capturedNote.id());
        assertEquals(content, capturedNote.content());
        assertEquals(testUserId, capturedNote.createdBy());
        assertNotNull(capturedNote.createdAt());
        assertNotNull(capturedNote.modifiedAt());
        assertEquals(capturedNote.createdAt(), capturedNote.modifiedAt());
    }

    @Test
    void save_withNullContent_shouldHandleGracefully() {
        // Given
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        Note savedNote = new Note("generated-ulid", (String) null, testUserId, now(), now());
        when(repository.save(any(Note.class))).thenReturn(savedNote);

        // When
        Note result = noteService.save(null, EncryptionMode.PUBLIC);

        // Then
        assertEquals(savedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertNull(capturedNote.content());
        assertEquals(testUserId, capturedNote.createdBy());
    }

    @Test
    void save_withEmptyContent_shouldCreateNote() {
        // Given
        String emptyContent = "";
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        Note savedNote = new Note("generated-ulid", emptyContent, testUserId, now(), now());
        when(repository.save(any(Note.class))).thenReturn(savedNote);

        // When
        Note result = noteService.save(emptyContent, EncryptionMode.PUBLIC);

        // Then
        assertEquals(savedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertEquals(emptyContent, capturedNote.content());
        assertEquals(testUserId, capturedNote.createdBy());
    }

    @Disabled
    @Test
    void update_whenNoteExists_shouldUpdateContentAndModifiedDate() {
        // Given
        String updatedContent = "Updated content content";
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        Note updatedNote = new Note(testId, updatedContent, testUserId, testCreatedAt, now());
        when(repository.save(any(Note.class))).thenReturn(updatedNote);

        // When
        Note result = noteService.update(testId, updatedContent, null, null);

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
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> noteService.update(testId, updatedContent, null, null));

        assertEquals("Note not found!", exception.getMessage());
        verify(repository).findById(testId);
        verify(repository, never()).save(any(Note.class));
    }

    @Test
    void update_withNullContent_shouldUpdateToNull() {
        // Given
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        Note updatedNote = new Note(testId, (String) null, testUserId, testCreatedAt, now());
        when(repository.save(any(Note.class))).thenReturn(updatedNote);

        // When
        Note result = noteService.update(testId, null, null, null);

        // Then
        assertEquals(updatedNote, result);
        verify(repository).save(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertNull(capturedNote.content());
    }

    @Test
    void findAll_withAuthenticatedUser_shouldReturnNoteIds() {
        // Given
        List<Note> notes = List.of(
            new Note("note1", "content1", testUserId, testCreatedAt, testModifiedAt),
            new Note("note2", "content2", testUserId, testCreatedAt, testModifiedAt)
        );
        when(repository.findAllByCreatedBy(testUserId)).thenReturn(notes);

        // When
        List<String> result = noteService.findAll();

        // Then
        assertEquals(List.of("note1", "note2"), result);
        verify(repository).findAllByCreatedBy(testUserId);
    }

    @Test
    void findAll_withAnonymousUser_shouldReturnEmptyList() {
        // Given
        UserContext anonymousUser = new UserContext("1", "anonymous", List.of());
        UserContextHolder.set(anonymousUser);

        // When
        List<String> result = noteService.findAll();

        // Then
        assertEquals(emptyList(), result);
        verify(repository, never()).findAllByCreatedBy(anyString());
    }

    @Test
    void findAll_withAuthenticatedUserNoNotes_shouldReturnEmptyList() {
        // Given
        when(repository.findAllByCreatedBy(testUserId)).thenReturn(emptyList());

        // When
        List<String> result = noteService.findAll();

        // Then
        assertEquals(emptyList(), result);
        verify(repository).findAllByCreatedBy(testUserId);
    }

    @Test
    void updateDisplay_asOwner_shouldSetColorAndPinnedWithoutTouchingContentOrModifiedAt() {
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));
        when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        Note result = noteService.updateDisplay(testId, "#FEFF9C", true);

        assertEquals("#feff9c", result.color());
        assertTrue(result.pinned());
        assertEquals(testContent, result.content());
        assertEquals(testModifiedAt, result.modifiedAt());
    }

    @Test
    void updateDisplay_withEmptyColor_shouldClearColor() {
        Note colored = testNote.withDisplay("#feff9c", false);
        when(repository.findById(testId)).thenReturn(Optional.of(colored));
        when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        Note result = noteService.updateDisplay(testId, "", null);

        assertNull(result.color());
        assertFalse(result.pinned());
    }

    @Test
    void updateDisplay_withInvalidColor_shouldThrowBadRequest() {
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));

        assertThrows(BadRequestException.class, () -> noteService.updateDisplay(testId, "red", null));
        verify(repository, never()).save(any(Note.class));
    }

    @Test
    void updateDisplay_asNonOwner_shouldThrowUnauthorized() {
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));
        UserContextHolder.set(new UserContext("someone-else", "other", List.of("USER")));

        assertThrows(UnauthorizedException.class, () -> noteService.updateDisplay(testId, "#feff9c", null));
        verify(repository, never()).save(any(Note.class));
    }

    @Test
    void updateDisplay_asAnonymous_shouldThrowUnauthorized() {
        when(repository.findById(testId)).thenReturn(Optional.of(testNote));
        UserContextHolder.set(new UserContext("1", "anonymous", List.of()));

        assertThrows(UnauthorizedException.class, () -> noteService.updateDisplay(testId, null, true));
    }

    @Test
    void update_shouldKeepExistingColorAndPinned() {
        Note colored = testNote.withDisplay("#98fb98", true);
        when(repository.findById(testId)).thenReturn(Optional.of(colored));
        when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        Note result = noteService.update(testId, "new body", null, null);

        assertEquals("#98fb98", result.color());
        assertTrue(result.pinned());
        assertEquals("new body", result.content());
    }

    @Test
    void findAllNotes_withAuthenticatedUser_shouldReturnFullNotes() {
        when(repository.findAllByCreatedBy(testUserId)).thenReturn(List.of(testNote));

        assertEquals(List.of(testNote), noteService.findAllNotes());
    }

    @Test
    void findAllNotes_withAnonymousUser_shouldReturnEmptyList() {
        UserContextHolder.set(new UserContext("1", "anonymous", List.of()));

        assertEquals(emptyList(), noteService.findAllNotes());
        verify(repository, never()).findAllByCreatedBy(anyString());
    }
}

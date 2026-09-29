package com.lhamacorp.knotes.service;

import com.github.f4b6a3.ulid.Ulid;
import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.exception.BadRequestException;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.f4b6a3.ulid.UlidCreator.getUlid;
import static com.lhamacorp.knotes.api.dto.NoteMetadata.from;
import static com.lhamacorp.knotes.domain.EncryptionMode.PRIVATE;
import static com.lhamacorp.knotes.domain.EncryptionMode.PUBLIC;
import static com.lhamacorp.knotes.domain.Note.ANONYMOUS;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;

@Service
public class NoteService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final NoteRepository repository;

    private static final String NOT_FOUND = "Note not found!";

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public boolean exists(String id) {
        return repository.existsById(id);
    }

    public Note get(String id) {
        return repository.findById(id).orElseThrow(() -> new BadRequestException("Note not found"));
    }

    public List<String> findAll() {
        UserContext user = UserContextHolder.get();
        return ANONYMOUS.equals(user.id())
                ? emptyList()
                : repository.findAllByCreatedBy(user.id()).stream().map(Note::id).toList();
    }

    @Cacheable(value = "content", key = "#id")
    public Note findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new BadRequestException(NOT_FOUND));
    }

    @Cacheable(value = "metadata", key = "#id")
    public NoteMetadata findMetadataById(String id) {
        Note noteProjection = repository.findMetadataById(id)
                .orElseThrow(() -> new BadRequestException(NOT_FOUND));
        return from(noteProjection);
    }

    public Note save(String content, EncryptionMode encryptionMode) {
        Ulid id = getUlid();
        UserContext user = UserContextHolder.get();
        Instant now = now();

        return repository.save(new Note(id.toString(), content, user.id(), now, now, encryptionMode, null));
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#id")
    public Note update(String id, String content, EncryptionMode encryptionMode, String password) {
        Note existingNote = repository.findById(id).orElseThrow(() -> new BadRequestException(NOT_FOUND));
        UserContext user = UserContextHolder.get();

        if (existingNote.encryptionMode() == PRIVATE && !existingNote.createdBy().equals(user.id())) {
            throw new UnauthorizedException("Not authorized to update this content");
        }

        if (ANONYMOUS.equals(user.id())) {
            encryptionMode = PUBLIC;
            password = null;
        }

        if (encryptionMode == null) {
            encryptionMode = existingNote.encryptionMode();
        }

        Note updated = new Note(id, content, existingNote.createdBy(), existingNote.createdAt(), now(), encryptionMode, password)
                .withDisplayFrom(existingNote);
        return repository.save(updated);
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#id")
    public Note updateDisplay(String id, String color, Boolean pinned) {
        Note existing = repository.findById(id).orElseThrow(() -> new BadRequestException(NOT_FOUND));
        UserContext user = UserContextHolder.get();

        if (ANONYMOUS.equals(user.id()) || !user.id().equals(existing.createdBy())) {
            throw new UnauthorizedException("Only the owner can change color or pin");
        }

        String newColor = existing.color();
        if (color != null) {
            if (!color.isEmpty() && !HEX_COLOR.matcher(color).matches()) {
                throw new BadRequestException("Color must be a hex value like #feff9c");
            }
            newColor = color.isEmpty() ? null : color.toLowerCase();
        }
        Boolean newPinned = pinned != null ? pinned : existing.pinned();

        return repository.save(existing.withDisplay(newColor, newPinned));
    }

    public List<Note> findAllNotes() {
        UserContext user = UserContextHolder.get();
        return ANONYMOUS.equals(user.id())
                ? emptyList()
                : repository.findAllByCreatedBy(user.id());
    }

    @CacheEvict(value = {"content", "metadata"}, key = "#id")
    public void delete(String id) {
        Note note = repository.findById(id).orElseThrow(() -> new BadRequestException("Note not found"));
        String userId = UserContextHolder.get().id();

        if (note.createdBy().equals(userId)) {
            repository.deleteById(note.id());
        }

    }

}

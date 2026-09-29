package com.lhamacorp.knotes.api;

import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.api.dto.NoteRequest;
import com.lhamacorp.knotes.api.dto.NoteResponse;
import com.lhamacorp.knotes.api.dto.NoteUpdateRequest;
import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.EncryptionMode;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

import static com.lhamacorp.knotes.context.UserContextHolder.isAuthenticated;
import static com.lhamacorp.knotes.domain.EncryptionMode.PRIVATE;
import static com.lhamacorp.knotes.domain.EncryptionMode.PUBLIC;
import static com.lhamacorp.knotes.domain.Note.ANONYMOUS;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;


    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<String>> findByUserId() {
        return ok(noteService.findAll());
    }

    /**
     * Full notes for the current user in one call (newest first), for card/list views.
     * Undecryptable notes come back with null content instead of failing the whole list.
     */
    @GetMapping(params = "expand=true")
    public ResponseEntity<List<NoteResponse>> findAllExpanded() {
        String userId = UserContextHolder.get().id();
        List<NoteResponse> notes = noteService.findAllNotes().stream()
                .sorted(Comparator.comparing(Note::modifiedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(note -> NoteResponse.summary(note, userId))
                .toList();
        return ok(notes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> findById(@PathVariable String id,
                                                 @RequestParam(required = false) String password) {
        UserContext user = UserContextHolder.get();
        Note note = noteService.findById(id);

        if (!canAccess(note, user.id(), password)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        return switch (note.encryptionMode()) {
            case PRIVATE -> ResponseEntity.ok(NoteResponse.fromPrivate(note, user.id()));
            case PASSWORD_SHARED -> ResponseEntity.ok(NoteResponse.fromPasswordShared(note, password));
            case PUBLIC -> ResponseEntity.ok(NoteResponse.from(note));
        };
    }

    @GetMapping("{id}/metadata")
    public ResponseEntity<NoteMetadata> getMetadata(@PathVariable String id) {
        NoteMetadata metadata = noteService.findMetadataById(id);
        return ok().body(metadata);
    }

    @PutMapping("{id}")
    public ResponseEntity<NoteResponse> update(@PathVariable String id,
                                               @RequestBody NoteUpdateRequest request,
                                               @RequestParam(required = false) String password) {
        UserContext user = UserContextHolder.get();
        String userId = user.id();

        if (ANONYMOUS.equals(userId) && request.encryptionMode() != null
                && !request.encryptionMode().equals("PUBLIC")) {
            return badRequest().build();
        }

        EncryptionMode mode = null;
        if (request.encryptionMode() != null && !request.encryptionMode().isEmpty()) {
            try {
                mode = EncryptionMode.valueOf(request.encryptionMode().toUpperCase());
            } catch (IllegalArgumentException e) {
                return badRequest().build();
            }
        }

        Note updatedNote = noteService.update(id, request.content(), mode, password);
        EncryptionMode finalMode = updatedNote.encryptionMode() != null ? updatedNote.encryptionMode() : PUBLIC;

        return switch (finalMode) {
            case PRIVATE -> ok().body(NoteResponse.fromPrivate(updatedNote, userId));
            case PASSWORD_SHARED -> ok().body(NoteResponse.fromPasswordShared(updatedNote, password));
            case PUBLIC -> ok().body(NoteResponse.from(updatedNote));
        };
    }

    @PostMapping
    public ResponseEntity<NoteResponse> save(@RequestBody NoteRequest request) {
        String userId = isAuthenticated() ? UserContextHolder.get().id() : ANONYMOUS;

        EncryptionMode mode = userId.equals(ANONYMOUS) ? PUBLIC : PRIVATE;
        Note savedNote = noteService.save(request.note(), mode);

        return switch (mode) {
            case PRIVATE -> ok().body(NoteResponse.fromPrivate(savedNote, userId));
            case PUBLIC -> ok().body(NoteResponse.from(savedNote));
            default -> throw new IllegalStateException("PASSWORD_SHARED not supported in this endpoint");
        };
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (isAuthenticated()) {
            noteService.delete(id);
        }

        return ok().build();
    }

    private boolean canAccess(Note note, String userId, String password) {
        return switch (note.encryptionMode()) {
            case PUBLIC -> true;
            case PRIVATE -> ANONYMOUS.equals(note.createdBy()) || userId.equals(note.createdBy());
            default -> false;
        };
    }

}

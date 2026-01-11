package com.lhamacorp.knotes.api;

import com.lhamacorp.knotes.api.dto.NoteMetadata;
import com.lhamacorp.knotes.api.dto.NoteRequest;
import com.lhamacorp.knotes.api.dto.NoteResponse;
import com.lhamacorp.knotes.api.dto.NoteUpdateRequest;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping("{id}")
    public ResponseEntity<NoteResponse> find(@PathVariable String id) {
        Note note = service.findById(id);
        return ResponseEntity.ok().body(NoteResponse.from(note));
    }

    @GetMapping("{id}/metadata")
    public ResponseEntity<NoteMetadata> getMetadata(@PathVariable String id) {
        NoteMetadata metadata = service.findMetadataById(id);
        return ResponseEntity.ok().body(metadata);
    }

    @PutMapping("{id}")
    public ResponseEntity<NoteResponse> update(@PathVariable String id, @RequestBody NoteUpdateRequest request) {
        Note updatedNote = service.update(id, request.content());
        return ResponseEntity.ok().body(NoteResponse.from(updatedNote));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> save(@RequestBody NoteRequest request) {
        Note savedNote = service.save(request.note());
        return ResponseEntity.ok().body(NoteResponse.from(savedNote));
    }

}

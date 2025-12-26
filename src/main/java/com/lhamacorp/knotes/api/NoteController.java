package com.lhamacorp.knotes.api;

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

    @PostMapping
    public ResponseEntity<NoteResponse> save(@RequestBody NoteRequest request) {
        Note savedNote = service.save(request.note());
        return ResponseEntity.ok().body(NoteResponse.from(savedNote));
    }

    @PutMapping
    public ResponseEntity<NoteResponse> update(@RequestBody NoteUpdateRequest request) {
        Note updatedNote = service.update(request.id(), request.content());
        return ResponseEntity.ok().body(NoteResponse.from(updatedNote));
    }

}

package com.lhamacorp.knotes.api;

import com.lhamacorp.knotes.api.dto.NoteRequest;
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
    public ResponseEntity<Note> find(@PathVariable String id) {
        return ResponseEntity.ok().body(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Note> save(@RequestBody NoteRequest request) {
        return ResponseEntity.ok().body(service.save(request.note()));
    }

    @PutMapping
    public ResponseEntity<Note> update(@RequestBody Note note) {
        return ResponseEntity.ok().body(service.update(note.id(), note.content()));
    }

}

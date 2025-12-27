package com.lhamacorp.knotes.service;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.exception.NotFoundException;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NoteService {

    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public boolean exists(String id) {
        return repository.existsById(id);
    }

    public Note findById(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Note with id " + id + " not found!"));
    }

    public Note save(String content) {
        Ulid id = UlidCreator.getUlid();
        Instant now = Instant.now();
        return repository.save(new Note(id.toString(), content, now, now));
    }

    public Note update(String id, String content) {
        Note note = findById(id);
        Instant now = Instant.now();
        return repository.save(new Note(id, content, note.createdAt(), now));
    }

}

package com.lhamacorp.knotes.service;

import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CleanupScheduler {

    private final NoteRepository repository;

    private static final String ONCE_PER_DAY_AT_2AM = "0 0 2 * * *";
    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    public CleanupScheduler(NoteRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = ONCE_PER_DAY_AT_2AM)
    public void cleanup() {
        List<Note> emptyNotes = repository.findEmptyNotes();
        List<String> ids = emptyNotes.stream()
            .map(Note::id)
            .toList();

        if (!ids.isEmpty()) {
            log.info("Cleaning empty notes [{}]", ids);
            repository.deleteAllById(ids);
        }
    }

}

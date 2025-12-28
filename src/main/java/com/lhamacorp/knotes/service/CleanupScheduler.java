package com.lhamacorp.knotes.service;

import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.repository.NoteRepository;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class CleanupScheduler {

    private final NoteRepository repository;

    private static final String ONCE_PER_DAY_AT_2AM = "* * * * * *";
    private static final Logger log = getLogger(CleanupScheduler.class);

    public CleanupScheduler(NoteRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = ONCE_PER_DAY_AT_2AM)
    public void cleanup() {
        List<String> ids = repository.findEmptyNotes()
            .stream()
            .filter(note -> note.createdAt().isBefore(now().minus(1, DAYS)))
            .map(Note::id)
            .toList();

        if (!ids.isEmpty()) {
            log.info("Cleaning empty notes [{}]", ids);
            repository.deleteAllById(ids);
        }
    }

}

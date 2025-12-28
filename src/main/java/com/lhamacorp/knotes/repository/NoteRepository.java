package com.lhamacorp.knotes.repository;

import com.lhamacorp.knotes.domain.Note;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends MongoRepository<Note, String> {

    @Query(value = "{ '_id': ?0 }", fields = "{ 'createdAt': 1, 'modifiedAt': 1 }")
    Optional<Note> findMetadataProjectionById(String id);

    @Query(value = "{ 'content': BinData(0, '') }", fields = "{ '_id': 1, 'createdAt': 1 }")
    List<Note> findEmptyNotes();
}

package com.lhamacorp.knotes.api.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the request shapes the API relies on. A display-only update ({"color": ...} / {"pinned": ...})
 * must come through with color/pinned set and content null; if the mapper dropped them, the controller
 * would fall back to the content-update path and erase the note.
 */
class NoteUpdateRequestJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void colorOnlyBody_keepsColorAndLeavesContentNull() throws Exception {
        NoteUpdateRequest request = mapper.readValue("{\"color\":\"#ff7eb9\"}", NoteUpdateRequest.class);

        assertEquals("#ff7eb9", request.color());
        assertNull(request.content());
        assertNull(request.encryptionMode());
        assertNull(request.pinned());
    }

    @Test
    void pinnedOnlyBody_keepsPinned() throws Exception {
        NoteUpdateRequest request = mapper.readValue("{\"pinned\":true}", NoteUpdateRequest.class);

        assertTrue(request.pinned());
        assertNull(request.content());
    }

    @Test
    void contentBody_stillWorks() throws Exception {
        NoteUpdateRequest request = mapper.readValue("{\"content\":\"hi\",\"encryptionMode\":\"PUBLIC\"}", NoteUpdateRequest.class);

        assertEquals("hi", request.content());
        assertEquals("PUBLIC", request.encryptionMode());
        assertNull(request.color());
    }
}

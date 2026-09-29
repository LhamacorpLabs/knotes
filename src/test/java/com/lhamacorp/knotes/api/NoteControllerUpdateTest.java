package com.lhamacorp.knotes.api;

import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.context.UserContextHolder;
import com.lhamacorp.knotes.domain.Note;
import com.lhamacorp.knotes.service.NoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Request-level guard for PUT /api/notes/{id}. A color or pin change must never reach the
 * content-update path, because that path rewrites the note body.
 */
class NoteControllerUpdateTest {

    private static final String USER = "user1";
    private static final String ID = "01ABCDEF1234567890ABCDEF12";

    private NoteService service;
    private MockMvc mvc;
    private Note existing;

    @BeforeEach
    void setUp() {
        service = mock(NoteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NoteController(service)).build();
        UserContextHolder.set(new UserContext(USER, "demo", List.of("USER")));
        Instant now = Instant.parse("2025-01-01T10:00:00Z");
        existing = new Note(ID, "keep me", USER, now, now).withDisplay("#feff9c", false);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void colorOnlyBody_updatesDisplayAndNeverTouchesContent() throws Exception {
        when(service.updateDisplay(eq(ID), eq("#ff7eb9"), any())).thenReturn(existing.withDisplay("#ff7eb9", false));

        mvc.perform(put("/api/notes/" + ID).contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"#ff7eb9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("keep me"))
                .andExpect(jsonPath("$.color").value("#ff7eb9"));

        verify(service).updateDisplay(eq(ID), eq("#ff7eb9"), any());
        verify(service, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void pinnedOnlyBody_updatesDisplayAndNeverTouchesContent() throws Exception {
        when(service.updateDisplay(eq(ID), any(), eq(true))).thenReturn(existing.withDisplay("#feff9c", true));

        mvc.perform(put("/api/notes/" + ID).contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("keep me"))
                .andExpect(jsonPath("$.pinned").value(true));

        verify(service, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void displayEndpoint_updatesDisplayOnly() throws Exception {
        when(service.updateDisplay(eq(ID), eq("#98fb98"), any())).thenReturn(existing.withDisplay("#98fb98", false));

        mvc.perform(put("/api/notes/" + ID + "/display").contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"#98fb98\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("keep me"));

        verify(service, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void displayEndpoint_withNothingToChange_isRejected() throws Exception {
        mvc.perform(put("/api/notes/" + ID + "/display").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).updateDisplay(anyString(), any(), any());
    }

    @Test
    void emptyBody_isRejectedInsteadOfBlankingTheNote() throws Exception {
        mvc.perform(put("/api/notes/" + ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(anyString(), any(), any(), any());
        verify(service, never()).updateDisplay(anyString(), any(), any());
    }

    @Test
    void contentBody_updatesContent() throws Exception {
        when(service.update(eq(ID), eq("new text"), any(), any())).thenReturn(existing);

        mvc.perform(put("/api/notes/" + ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"new text\",\"encryptionMode\":\"PUBLIC\"}"))
                .andExpect(status().isOk());

        verify(service).update(eq(ID), eq("new text"), any(), any());
        verify(service, never()).updateDisplay(anyString(), any(), any());
    }

    @Test
    void emptyStringContent_isAValidContentUpdate() throws Exception {
        when(service.update(eq(ID), eq(""), any(), any())).thenReturn(existing);

        mvc.perform(put("/api/notes/" + ID).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"\"}"))
                .andExpect(status().isOk());

        verify(service).update(eq(ID), eq(""), any(), any());
    }
}

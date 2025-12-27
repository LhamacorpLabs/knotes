package com.lhamacorp.knotes.web;

import com.lhamacorp.knotes.service.NoteService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    private final NoteService service;

    public WebController(NoteService service) {
        this.service = service;
    }

    /**
     * Serve the home page at root path
     */
    @GetMapping("/")
    public String serveHomePage() {
        return "forward:/home.html";
    }

    /**
     * Handle note ID paths by serving the index.html file
     * Matches any alphanumeric ID and forwards to 404 if not found
     */
    @GetMapping("/{noteId:[A-Za-z0-9]+}")
    public String serveNoteByPath(@PathVariable String noteId) {
        return service.exists(noteId)
                ? "forward:/index.html"
                : "forward:/404.html";
    }

}
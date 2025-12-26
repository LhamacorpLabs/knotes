package com.lhamacorp.knotes.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    /**
     * Handle note ID paths by serving the index.html file
     * Matches note IDs which are typically 26-character ULIDs
     */
    @GetMapping("/{noteId:[A-Za-z0-9]{26}}")
    public String serveNoteByPath(@PathVariable String noteId) {
        return "forward:/index.html";
    }
}
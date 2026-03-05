package com.example.crm.controller;

import com.example.crm.service.InstantlyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing Unibox-related endpoints (replies/inbox) backed by InstantlyService.
 */
@RestController
@RequestMapping("/api")
public class UniboxController {

    private final InstantlyService instantlyService;

    @Autowired
    public UniboxController(InstantlyService instantlyService) {
        this.instantlyService = instantlyService;
    }

    /**
     * GET /api/unibox
     * Returns Unibox email threads (replies) from Instantly. Optional `limit` query param.
     */
    @GetMapping("/unibox")
    public ResponseEntity<String> getUnibox(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return instantlyService.fetchUniboxEmails(limit);
    }

    /**
     * GET /api/unibox/thread?thread_id={thread_id}
     * Returns a single email thread from Instantly identified by `thread_id`.
     */
    @GetMapping("/unibox/thread")
    public ResponseEntity<String> getUniboxThread(@RequestParam(value = "thread_id") String threadId) {
        return instantlyService.fetchEmailThread(threadId);
    }

    /**
     * GET /api/unibox/thread-by-email?email={lead_email}
     * Returns JSON with `thread_id` for the given lead email.
     */
    @GetMapping("/unibox/thread-by-email")
    public ResponseEntity<String> getThreadByEmail(@RequestParam(value = "email") String email) {
        return instantlyService.fetchThreadIdByEmail(email);
    }

}

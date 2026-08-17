package com.fraudwatch.controller;

import com.fraudwatch.service.TransactionIngestionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionIngestionService ingestionService;

    public TransactionController(TransactionIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Loads a CSV instantly (no delay) - useful for seeding history before a replay demo.
     * Invalid rows (zero/negative amount, blank accountId) are skipped rather than failing
     * the whole upload; the response reports how many rows were skipped.
     */
    @PostMapping("/ingest")
    public TransactionIngestionService.IngestResult ingest(@RequestParam("file") MultipartFile file) throws Exception {
        return ingestionService.ingestBulk(file.getInputStream());
    }

    /**
     * Kicks off a paced replay of the CSV, broadcasting each transaction (and its
     * anomaly verdict) over the /ws/transactions WebSocket as it's processed.
     */
    @PostMapping("/replay")
    public Map<String, Object> replay(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "delayMs", defaultValue = "400") long delayMs) throws Exception {
        if (ingestionService.isReplayInProgress()) {
            return Map.of("started", false, "reason", "A replay is already in progress");
        }
        byte[] bytes = file.getBytes();
        ingestionService.replayAsync(new ByteArrayInputStream(bytes), delayMs);
        return Map.of("started", true);
    }

    /** "lastReplaySkippedCount" reflects the most recently completed (or in-progress) replay. */
    @GetMapping("/replay/status")
    public Map<String, Object> replayStatus() {
        return Map.of(
                "inProgress", ingestionService.isReplayInProgress(),
                "lastReplaySkippedCount", ingestionService.getLastReplaySkippedCount()
        );
    }
}

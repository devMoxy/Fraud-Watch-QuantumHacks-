package com.fraudwatch.service;

import com.fraudwatch.dto.TransactionEvent;
import com.fraudwatch.model.AnomalyFlag;
import com.fraudwatch.model.Transaction;
import com.fraudwatch.repository.TransactionRepository;
import com.fraudwatch.websocket.TransactionWebSocketHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TransactionIngestionService {

    private final CsvTransactionParser csvParser;
    private final TransactionRepository transactionRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final TransactionWebSocketHandler webSocketHandler;

    private final AtomicBoolean replayInProgress = new AtomicBoolean(false);
    private final AtomicInteger lastReplaySkippedCount = new AtomicInteger(0);

    public TransactionIngestionService(CsvTransactionParser csvParser,
                                        TransactionRepository transactionRepository,
                                        AnomalyDetectionService anomalyDetectionService,
                                        TransactionWebSocketHandler webSocketHandler) {
        this.csvParser = csvParser;
        this.transactionRepository = transactionRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.webSocketHandler = webSocketHandler;
    }

    /** Bulk-loads a CSV instantly, no delay, no broadcast. Good for seeding baseline history. */
    public IngestResult ingestBulk(InputStream csvStream) throws Exception {
        CsvTransactionParser.ParseResult parsed = csvParser.parse(csvStream);
        List<TransactionEvent> results = new ArrayList<>();
        for (Transaction t : parsed.getTransactions()) {
            results.add(processOne(t));
        }
        return new IngestResult(results, parsed.getSkippedCount());
    }

    /**
     * Replays a CSV as a live stream: persists + scores one transaction at a time with a
     * delay between each, broadcasting every result over the WebSocket. This is what
     * powers the "Replay transactions" demo button on the dashboard.
     */
    @Async
    public void replayAsync(InputStream csvStream, long delayMillis) {
        if (!replayInProgress.compareAndSet(false, true)) {
            return; // a replay is already running
        }
        try {
            CsvTransactionParser.ParseResult parsed = csvParser.parse(csvStream);
            lastReplaySkippedCount.set(parsed.getSkippedCount());
            for (Transaction t : parsed.getTransactions()) {
                TransactionEvent event = processOne(t);
                webSocketHandler.broadcast(event);
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException("Replay failed", e);
        } finally {
            replayInProgress.set(false);
        }
    }

    public boolean isReplayInProgress() {
        return replayInProgress.get();
    }

    /** Number of rows skipped by the most recently completed (or in-progress) replay. */
    public int getLastReplaySkippedCount() {
        return lastReplaySkippedCount.get();
    }

    private TransactionEvent processOne(Transaction t) {
        Transaction saved = transactionRepository.save(t);
        Optional<AnomalyFlag> flag = anomalyDetectionService.evaluate(saved);
        return TransactionEvent.of(saved, flag.orElse(null));
    }

    /** Result of a bulk ingest: the processed events plus how many rows were skipped as invalid. */
    public static class IngestResult {
        private final List<TransactionEvent> transactions;
        private final int skippedCount;

        public IngestResult(List<TransactionEvent> transactions, int skippedCount) {
            this.transactions = transactions;
            this.skippedCount = skippedCount;
        }

        public List<TransactionEvent> getTransactions() {
            return transactions;
        }

        public int getSkippedCount() {
            return skippedCount;
        }
    }
}

package com.fraudwatch.controller;

import com.fraudwatch.dto.TransactionEvent;
import com.fraudwatch.model.AnomalyFlag;
import com.fraudwatch.model.Transaction;
import com.fraudwatch.repository.AnomalyFlagRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final TransactionRepository transactionRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;

    public DashboardController(TransactionRepository transactionRepository,
                                AnomalyFlagRepository anomalyFlagRepository) {
        this.transactionRepository = transactionRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
    }

    @GetMapping("/recent")
    public List<TransactionEvent> recentTransactions() {
        List<Transaction> transactions = transactionRepository.findTop100ByOrderByTimestampDesc();
        Map<Long, AnomalyFlag> flagsByTxnId = anomalyFlagRepository.findTop100ByOrderByFlaggedAtDesc().stream()
                .collect(Collectors.toMap(f -> f.getTransaction().getId(), f -> f, (a, b) -> a));

        return transactions.stream()
                .map(t -> TransactionEvent.of(t, flagsByTxnId.get(t.getId())))
                .toList();
    }

    @GetMapping("/flags")
    public List<TransactionEvent> flaggedTransactions() {
        return anomalyFlagRepository.findTop100ByOrderByFlaggedAtDesc().stream()
                .map(f -> TransactionEvent.of(f.getTransaction(), f))
                .toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long totalTxns = transactionRepository.count();
        long totalFlags = anomalyFlagRepository.count();
        double flagRate = totalTxns == 0 ? 0 : (double) totalFlags / totalTxns * 100;

        Map<String, Long> byReason = anomalyFlagRepository.findAll().stream()
                .collect(Collectors.groupingBy(f -> f.getReason().name(), Collectors.counting()));

        return Map.of(
                "totalTransactions", totalTxns,
                "totalFlags", totalFlags,
                "flagRatePercent", Math.round(flagRate * 100.0) / 100.0,
                "flagsByReason", byReason
        );
    }
}

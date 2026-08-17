package com.fraudwatch.service;

import com.fraudwatch.model.AnomalyFlag;
import com.fraudwatch.model.Transaction;
import com.fraudwatch.repository.AnomalyFlagRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scores each incoming transaction against the account's own recent history.
 *
 * Signals combined (first one to trip wins, they're checked in order of
 * how "hard" a signal they are):
 *   1. Velocity      - too many transactions for this account in a short window
 *   2. Z-score        - amount is a statistical outlier vs. the account's rolling mean/stddev
 *   3. Large deviation - amount is a large multiple of the rolling mean (catches early accounts
 *                         with too few points for a stable stddev)
 *   4. New location   - first time this account has transacted from this location
 *   5. New category   - first time this account has transacted in this merchant category
 */
@Service
public class AnomalyDetectionService {

    private final TransactionRepository transactionRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;

    private final double zScoreThreshold;
    private final int rollingWindowSize;
    private final double largeAmountMultiplier;
    private final int velocityWindowSeconds;
    private final int velocityMaxCount;

    // per-account set of previously seen locations (in-memory cache, rebuilt from DB lazily)
    private final ConcurrentHashMap<String, Set<String>> knownLocationsByAccount = new ConcurrentHashMap<>();

    // per-account set of previously seen merchant categories (in-memory cache, rebuilt from DB lazily)
    private final ConcurrentHashMap<String, Set<String>> knownCategoriesByAccount = new ConcurrentHashMap<>();

    public AnomalyDetectionService(
            TransactionRepository transactionRepository,
            AnomalyFlagRepository anomalyFlagRepository,
            @Value("${fraudwatch.detection.z-score-threshold}") double zScoreThreshold,
            @Value("${fraudwatch.detection.rolling-window-size}") int rollingWindowSize,
            @Value("${fraudwatch.detection.large-amount-multiplier}") double largeAmountMultiplier,
            @Value("${fraudwatch.detection.velocity-window-seconds}") int velocityWindowSeconds,
            @Value("${fraudwatch.detection.velocity-max-count}") int velocityMaxCount) {
        this.transactionRepository = transactionRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.zScoreThreshold = zScoreThreshold;
        this.rollingWindowSize = rollingWindowSize;
        this.largeAmountMultiplier = largeAmountMultiplier;
        this.velocityWindowSeconds = velocityWindowSeconds;
        this.velocityMaxCount = velocityMaxCount;
    }

    /**
     * Evaluate a transaction that has already been persisted. Returns an unsaved
     * AnomalyFlag if the transaction looks suspicious, otherwise empty.
     */
    public Optional<AnomalyFlag> evaluate(Transaction txn) {
        List<Transaction> history = transactionRepository
                .findTop50ByAccountIdOrderByTimestampDesc(txn.getAccountId())
                .stream()
                .filter(t -> !t.getId().equals(txn.getId()))
                .toList();

        Optional<AnomalyFlag> velocityFlag = checkVelocity(txn, history);
        if (velocityFlag.isPresent()) {
            return persist(velocityFlag.get());
        }

        if (history.size() >= 3) {
            List<BigDecimal> window = history.stream()
                    .limit(rollingWindowSize)
                    .map(Transaction::getAmount)
                    .toList();

            double mean = mean(window);
            double stdDev = stdDev(window, mean);
            double amount = txn.getAmount().doubleValue();

            if (stdDev > 0.01) {
                double zScore = (amount - mean) / stdDev;
                if (Math.abs(zScore) >= zScoreThreshold) {
                    double risk = Math.min(100, 50 + Math.abs(zScore) * 10);
                    AnomalyFlag flag = new AnomalyFlag(txn, AnomalyFlag.AnomalyReason.Z_SCORE_OUTLIER, risk,
                            String.format("Amount %.2f is %.1f std-devs from account's rolling mean of %.2f",
                                    amount, zScore, mean),
                            Instant.now());
                    return persist(flag);
                }
            } else if (amount > mean * largeAmountMultiplier && mean > 0) {
                double risk = Math.min(100, 40 + (amount / Math.max(mean, 1)) * 5);
                AnomalyFlag flag = new AnomalyFlag(txn, AnomalyFlag.AnomalyReason.LARGE_AMOUNT_DEVIATION, risk,
                        String.format("Amount %.2f is %.1fx the account's rolling mean of %.2f",
                                amount, amount / mean, mean),
                        Instant.now());
                return persist(flag);
            }
        }

        Optional<AnomalyFlag> locationFlag = checkNewLocation(txn, history);
        if (locationFlag.isPresent()) {
            return persist(locationFlag.get());
        }

        // still record this account/location combo as seen even if not flagged
        knownLocationsByAccount
                .computeIfAbsent(txn.getAccountId(), k -> ConcurrentHashMap.newKeySet())
                .add(txn.getLocation());

        Optional<AnomalyFlag> categoryFlag = checkNewCategory(txn, history);
        if (categoryFlag.isPresent()) {
            return persist(categoryFlag.get());
        }

        // still record this account/category combo as seen even if not flagged
        knownCategoriesByAccount
                .computeIfAbsent(txn.getAccountId(), k -> ConcurrentHashMap.newKeySet())
                .add(txn.getCategory());

        return Optional.empty();
    }

    private Optional<AnomalyFlag> checkVelocity(Transaction txn, List<Transaction> history) {
        Instant windowStart = txn.getTimestamp().minus(Duration.ofSeconds(velocityWindowSeconds));
        long countInWindow = history.stream()
                .filter(t -> !t.getTimestamp().isBefore(windowStart))
                .count() + 1; // include current txn

        if (countInWindow > velocityMaxCount) {
            double risk = Math.min(100, 50 + (countInWindow - velocityMaxCount) * 8);
            return Optional.of(new AnomalyFlag(txn, AnomalyFlag.AnomalyReason.HIGH_VELOCITY, risk,
                    String.format("%d transactions for account %s within %ds (limit %d)",
                            countInWindow, txn.getAccountId(), velocityWindowSeconds, velocityMaxCount),
                    Instant.now()));
        }
        return Optional.empty();
    }

    private Optional<AnomalyFlag> checkNewLocation(Transaction txn, List<Transaction> history) {
        if (history.size() < 5) {
            return Optional.empty(); // not enough history to know what's "normal" yet
        }
        Set<String> known = knownLocationsByAccount.computeIfAbsent(txn.getAccountId(), k -> {
            Set<String> set = ConcurrentHashMap.newKeySet();
            history.forEach(t -> set.add(t.getLocation()));
            return set;
        });
        if (!known.contains(txn.getLocation())) {
            return Optional.of(new AnomalyFlag(txn, AnomalyFlag.AnomalyReason.NEW_LOCATION, 45,
                    String.format("First transaction for account %s from location %s",
                            txn.getAccountId(), txn.getLocation()),
                    Instant.now()));
        }
        return Optional.empty();
    }

    private Optional<AnomalyFlag> checkNewCategory(Transaction txn, List<Transaction> history) {
        if (history.size() < 5) {
            return Optional.empty(); // not enough history to know what's "normal" yet
        }
        Set<String> known = knownCategoriesByAccount.computeIfAbsent(txn.getAccountId(), k -> {
            Set<String> set = ConcurrentHashMap.newKeySet();
            history.forEach(t -> set.add(t.getCategory()));
            return set;
        });
        if (!known.contains(txn.getCategory())) {
            return Optional.of(new AnomalyFlag(txn, AnomalyFlag.AnomalyReason.MERCHANT_CATEGORY_ANOMALY, 40,
                    String.format("First transaction for account %s in category %s",
                            txn.getAccountId(), txn.getCategory()),
                    Instant.now()));
        }
        return Optional.empty();
    }

    private Optional<AnomalyFlag> persist(AnomalyFlag flag) {
        return Optional.of(anomalyFlagRepository.save(flag));
    }

    private double mean(List<BigDecimal> values) {
        return values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
    }

    private double stdDev(List<BigDecimal> values, double mean) {
        if (values.size() < 2) return 0;
        double sumSq = values.stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
                .sum();
        return Math.sqrt(sumSq / (values.size() - 1));
    }
}

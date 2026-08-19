package com.fraudwatch.service;

import com.fraudwatch.model.AnomalyFlag;
import com.fraudwatch.model.Transaction;
import com.fraudwatch.repository.AnomalyFlagRepository;
import com.fraudwatch.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit tests for {@link AnomalyDetectionService}, with the repositories mocked
 * out so these run without a Spring context or a database.
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final int ROLLING_WINDOW_SIZE = 20;
    private static final double LARGE_AMOUNT_MULTIPLIER = 5.0;
    private static final int VELOCITY_WINDOW_SECONDS = 60;
    private static final int VELOCITY_MAX_COUNT = 5;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AnomalyFlagRepository anomalyFlagRepository;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyDetectionService(
                transactionRepository,
                anomalyFlagRepository,
                Z_SCORE_THRESHOLD,
                ROLLING_WINDOW_SIZE,
                LARGE_AMOUNT_MULTIPLIER,
                VELOCITY_WINDOW_SECONDS,
                VELOCITY_MAX_COUNT);

        // not every test trips a flag, so this stub is only consumed by some of them
        lenient().when(anomalyFlagRepository.save(any(AnomalyFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void flagsTransactionThatIsAStatisticalOutlier() {
        String accountId = "acc-zscore";
        Instant now = Instant.now();

        List<Transaction> history = List.of(
                txn(1L, accountId, 50.00, "NYC", "GROCERY", now.minus(10, ChronoUnit.DAYS)),
                txn(2L, accountId, 52.00, "NYC", "GROCERY", now.minus(9, ChronoUnit.DAYS)),
                txn(3L, accountId, 48.00, "NYC", "GROCERY", now.minus(8, ChronoUnit.DAYS)),
                txn(4L, accountId, 51.00, "NYC", "GROCERY", now.minus(7, ChronoUnit.DAYS)));
        when(transactionRepository.findTop50ByAccountIdOrderByTimestampDesc(accountId)).thenReturn(history);

        Transaction current = txn(5L, accountId, 500.00, "NYC", "GROCERY", now);

        Optional<AnomalyFlag> result = service.evaluate(current);

        assertTrue(result.isPresent());
        assertEquals(AnomalyFlag.AnomalyReason.Z_SCORE_OUTLIER, result.get().getReason());
    }

    @Test
    void flagsTransactionThatIsALargeMultipleOfTheRollingMean() {
        String accountId = "acc-large-amount";
        Instant now = Instant.now();

        // amounts identical -> stddev ~0, so the detector falls back to the large-amount check
        List<Transaction> history = List.of(
                txn(1L, accountId, 20.00, "NYC", "GROCERY", now.minus(10, ChronoUnit.DAYS)),
                txn(2L, accountId, 20.00, "NYC", "GROCERY", now.minus(9, ChronoUnit.DAYS)),
                txn(3L, accountId, 20.00, "NYC", "GROCERY", now.minus(8, ChronoUnit.DAYS)),
                txn(4L, accountId, 20.00, "NYC", "GROCERY", now.minus(7, ChronoUnit.DAYS)));
        when(transactionRepository.findTop50ByAccountIdOrderByTimestampDesc(accountId)).thenReturn(history);

        Transaction current = txn(5L, accountId, 150.00, "NYC", "GROCERY", now);

        Optional<AnomalyFlag> result = service.evaluate(current);

        assertTrue(result.isPresent());
        assertEquals(AnomalyFlag.AnomalyReason.LARGE_AMOUNT_DEVIATION, result.get().getReason());
    }

    @Test
    void flagsTransactionThatExceedsTheVelocityLimit() {
        String accountId = "acc-velocity";
        Instant now = Instant.now();

        // 5 prior transactions inside the 60s window, plus the current one, exceeds the limit of 5
        List<Transaction> history = List.of(
                txn(1L, accountId, 10.00, "NYC", "GROCERY", now.minus(10, ChronoUnit.SECONDS)),
                txn(2L, accountId, 10.00, "NYC", "GROCERY", now.minus(20, ChronoUnit.SECONDS)),
                txn(3L, accountId, 10.00, "NYC", "GROCERY", now.minus(30, ChronoUnit.SECONDS)),
                txn(4L, accountId, 10.00, "NYC", "GROCERY", now.minus(40, ChronoUnit.SECONDS)),
                txn(5L, accountId, 10.00, "NYC", "GROCERY", now.minus(50, ChronoUnit.SECONDS)));
        when(transactionRepository.findTop50ByAccountIdOrderByTimestampDesc(accountId)).thenReturn(history);

        Transaction current = txn(6L, accountId, 10.00, "NYC", "GROCERY", now);

        Optional<AnomalyFlag> result = service.evaluate(current);

        assertTrue(result.isPresent());
        assertEquals(AnomalyFlag.AnomalyReason.HIGH_VELOCITY, result.get().getReason());
    }

    @Test
    void flagsTransactionFromANewLocation() {
        String accountId = "acc-new-location";
        Instant now = Instant.now();

        // enough history to establish "known" locations, all far outside the velocity window,
        // with an amount close enough to the mean to avoid tripping the z-score check
        List<Transaction> history = List.of(
                txn(1L, accountId, 50.00, "NYC", "GROCERY", now.minus(10, ChronoUnit.DAYS)),
                txn(2L, accountId, 52.00, "NYC", "GROCERY", now.minus(9, ChronoUnit.DAYS)),
                txn(3L, accountId, 49.00, "NYC", "GROCERY", now.minus(8, ChronoUnit.DAYS)),
                txn(4L, accountId, 51.00, "NYC", "GROCERY", now.minus(7, ChronoUnit.DAYS)),
                txn(5L, accountId, 50.00, "NYC", "GROCERY", now.minus(6, ChronoUnit.DAYS)));
        when(transactionRepository.findTop50ByAccountIdOrderByTimestampDesc(accountId)).thenReturn(history);

        Transaction current = txn(6L, accountId, 50.00, "LA", "GROCERY", now);

        Optional<AnomalyFlag> result = service.evaluate(current);

        assertTrue(result.isPresent());
        assertEquals(AnomalyFlag.AnomalyReason.NEW_LOCATION, result.get().getReason());
    }

    @Test
    void doesNotFlagANormalTransaction() {
        String accountId = "acc-normal";
        Instant now = Instant.now();

        // consistent amount, known location, known category, and well outside the velocity window
        List<Transaction> history = List.of(
                txn(1L, accountId, 50.00, "NYC", "GROCERY", now.minus(10, ChronoUnit.DAYS)),
                txn(2L, accountId, 52.00, "NYC", "GROCERY", now.minus(9, ChronoUnit.DAYS)),
                txn(3L, accountId, 49.00, "NYC", "GROCERY", now.minus(8, ChronoUnit.DAYS)),
                txn(4L, accountId, 51.00, "NYC", "GROCERY", now.minus(7, ChronoUnit.DAYS)),
                txn(5L, accountId, 50.00, "NYC", "GROCERY", now.minus(6, ChronoUnit.DAYS)));
        when(transactionRepository.findTop50ByAccountIdOrderByTimestampDesc(accountId)).thenReturn(history);

        Transaction current = txn(6L, accountId, 50.00, "NYC", "GROCERY", now);

        Optional<AnomalyFlag> result = service.evaluate(current);

        assertTrue(result.isEmpty());
        verify(anomalyFlagRepository, never()).save(any(AnomalyFlag.class));
    }

    private static Transaction txn(long id, String accountId, double amount, String location,
                                    String category, Instant timestamp) {
        Transaction transaction = new Transaction(
                accountId, BigDecimal.valueOf(amount), "USD", "Test Merchant", category, location, timestamp);
        transaction.setId(id);
        return transaction;
    }
}

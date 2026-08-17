package com.fraudwatch.repository;

import com.fraudwatch.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findTop50ByAccountIdOrderByTimestampDesc(String accountId);

    List<Transaction> findByAccountIdAndTimestampAfterOrderByTimestampAsc(String accountId, Instant after);

    List<Transaction> findTop100ByOrderByTimestampDesc();
}

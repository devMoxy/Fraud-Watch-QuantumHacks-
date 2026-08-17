package com.fraudwatch.service;

import com.fraudwatch.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Expected CSV header: accountId,amount,currency,merchant,category,location,timestamp
 * timestamp must be ISO-8601 (e.g. 2026-08-17T10:15:30Z)
 */
@Component
public class CsvTransactionParser {

    private static final Logger log = LoggerFactory.getLogger(CsvTransactionParser.class);

    public ParseResult parse(InputStream inputStream) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        int skippedCount = 0;
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    transactions.add(parseRow(record));
                } catch (Exception e) {
                    skippedCount++;
                    log.warn("Skipping CSV row {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }
        }
        return new ParseResult(transactions, skippedCount);
    }

    private Transaction parseRow(CSVRecord record) {
        String accountId = record.get("accountId");
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId is blank");
        }
        BigDecimal amount = new BigDecimal(record.get("amount"));
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
        return new Transaction(
                accountId,
                amount,
                record.get("currency"),
                record.get("merchant"),
                record.get("category"),
                record.get("location"),
                Instant.parse(record.get("timestamp"))
        );
    }

    /** Parsed transactions plus how many rows were rejected as invalid. */
    public static class ParseResult {
        private final List<Transaction> transactions;
        private final int skippedCount;

        public ParseResult(List<Transaction> transactions, int skippedCount) {
            this.transactions = transactions;
            this.skippedCount = skippedCount;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public int getSkippedCount() {
            return skippedCount;
        }
    }
}

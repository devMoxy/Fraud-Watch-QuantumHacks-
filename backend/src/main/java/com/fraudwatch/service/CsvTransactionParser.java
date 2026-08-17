package com.fraudwatch.service;

import com.fraudwatch.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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

    public List<Transaction> parse(InputStream inputStream) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Transaction t = new Transaction(
                        record.get("accountId"),
                        new BigDecimal(record.get("amount")),
                        record.get("currency"),
                        record.get("merchant"),
                        record.get("category"),
                        record.get("location"),
                        Instant.parse(record.get("timestamp"))
                );
                transactions.add(t);
            }
        }
        return transactions;
    }
}

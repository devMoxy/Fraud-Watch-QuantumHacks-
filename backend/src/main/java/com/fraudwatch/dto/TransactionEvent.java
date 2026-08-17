package com.fraudwatch.dto;

import com.fraudwatch.model.AnomalyFlag;
import com.fraudwatch.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Flattened view of a transaction plus (optional) anomaly verdict, sent to the
 * frontend both over the initial REST fetch and over the live WebSocket feed.
 */
public class TransactionEvent {

    private Long id;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String merchant;
    private String category;
    private String location;
    private Instant timestamp;

    private boolean flagged;
    private String reason;
    private double riskScore;
    private String detail;

    public static TransactionEvent of(Transaction t, AnomalyFlag flag) {
        TransactionEvent e = new TransactionEvent();
        e.id = t.getId();
        e.accountId = t.getAccountId();
        e.amount = t.getAmount();
        e.currency = t.getCurrency();
        e.merchant = t.getMerchant();
        e.category = t.getCategory();
        e.location = t.getLocation();
        e.timestamp = t.getTimestamp();
        if (flag != null) {
            e.flagged = true;
            e.reason = flag.getReason().name();
            e.riskScore = flag.getRiskScore();
            e.detail = flag.getDetail();
        }
        return e;
    }

    public Long getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public String getReason() {
        return reason;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getDetail() {
        return detail;
    }
}

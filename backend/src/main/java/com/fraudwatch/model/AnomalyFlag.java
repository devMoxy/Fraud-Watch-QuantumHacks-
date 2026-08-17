package com.fraudwatch.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "anomaly_flags")
public class AnomalyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyReason reason;

    @Column(nullable = false)
    private double riskScore; // 0-100

    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private Instant flaggedAt;

    public AnomalyFlag() {
    }

    public AnomalyFlag(Transaction transaction, AnomalyReason reason, double riskScore, String detail, Instant flaggedAt) {
        this.transaction = transaction;
        this.reason = reason;
        this.riskScore = riskScore;
        this.detail = detail;
        this.flaggedAt = flaggedAt;
    }

    public Long getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public AnomalyReason getReason() {
        return reason;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getFlaggedAt() {
        return flaggedAt;
    }

    public enum AnomalyReason {
        Z_SCORE_OUTLIER,
        LARGE_AMOUNT_DEVIATION,
        HIGH_VELOCITY,
        NEW_LOCATION,
        MERCHANT_CATEGORY_ANOMALY
    }
}

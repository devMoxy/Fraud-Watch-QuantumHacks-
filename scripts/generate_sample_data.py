#!/usr/bin/env python3
"""
Generates sample-data/transactions.csv: realistic per-account spending patterns
with a handful of seeded anomalies (amount outliers, velocity bursts, new
locations) sprinkled in near the end so the "Replay transactions" demo has
something to flag.

Usage: python3 scripts/generate_sample_data.py
"""
import csv
import random
from datetime import datetime, timedelta, timezone

random.seed(42)

ACCOUNTS = {
    "acct-1001": {"mean": 45, "stddev": 15, "home_locations": ["London", "Manchester"]},
    "acct-1002": {"mean": 120, "stddev": 30, "home_locations": ["New York", "Boston"]},
    "acct-1003": {"mean": 20, "stddev": 8, "home_locations": ["Berlin"]},
    "acct-1004": {"mean": 300, "stddev": 60, "home_locations": ["Singapore", "Hong Kong"]},
    "acct-1005": {"mean": 60, "stddev": 20, "home_locations": ["Toronto"]},
}

MERCHANTS = ["Amazon", "Tesco", "Shell", "Starbucks", "Uber", "Netflix", "Apple",
             "Delta Airlines", "Best Buy", "Steam", "Airbnb", "Zara"]
CATEGORIES = ["Retail", "Groceries", "Fuel", "Dining", "Transport", "Subscription",
              "Electronics", "Travel", "Entertainment"]
CURRENCY = "USD"

rows = []
start = datetime(2026, 8, 14, 9, 0, 0, tzinfo=timezone.utc)
t = start

for account_id, profile in ACCOUNTS.items():
    account_time = start + timedelta(minutes=random.randint(0, 120))
    for i in range(60):
        amount = max(1.0, random.gauss(profile["mean"], profile["stddev"]))
        location = random.choice(profile["home_locations"])
        rows.append({
            "accountId": account_id,
            "amount": f"{amount:.2f}",
            "currency": CURRENCY,
            "merchant": random.choice(MERCHANTS),
            "category": random.choice(CATEGORIES),
            "location": location,
            "timestamp": account_time.strftime("%Y-%m-%dT%H:%M:%SZ"),
        })
        account_time += timedelta(minutes=random.randint(15, 240))

# --- Seeded anomalies, appended so they land late in the replay ---
anomaly_time = start + timedelta(days=2, hours=3)

# 1. Amount outlier: acct-1001 normally spends ~$45, this one is $2,400
rows.append({
    "accountId": "acct-1001", "amount": "2400.00", "currency": CURRENCY,
    "merchant": "Luxury Watches Co", "category": "Retail", "location": "London",
    "timestamp": anomaly_time.strftime("%Y-%m-%dT%H:%M:%SZ"),
})

# 2. New location: acct-1003 (Berlin-only) suddenly transacts from Lagos
anomaly_time += timedelta(minutes=10)
rows.append({
    "accountId": "acct-1003", "amount": "89.50", "currency": CURRENCY,
    "merchant": "Unknown Merchant", "category": "Electronics", "location": "Lagos",
    "timestamp": anomaly_time.strftime("%Y-%m-%dT%H:%M:%SZ"),
})

# 3. Velocity burst: acct-1004 fires 8 transactions in under a minute
burst_time = anomaly_time + timedelta(minutes=15)
for i in range(8):
    rows.append({
        "accountId": "acct-1004", "amount": f"{random.uniform(50, 150):.2f}", "currency": CURRENCY,
        "merchant": random.choice(MERCHANTS), "category": "Retail", "location": "Singapore",
        "timestamp": (burst_time + timedelta(seconds=i * 5)).strftime("%Y-%m-%dT%H:%M:%SZ"),
    })

# 4. Large-amount deviation on a thin-history account: acct-1005 spikes early
anomaly_time2 = start + timedelta(hours=6)
rows.append({
    "accountId": "acct-1005", "amount": "980.00", "currency": CURRENCY,
    "merchant": "Best Buy", "category": "Electronics", "location": "Toronto",
    "timestamp": anomaly_time2.strftime("%Y-%m-%dT%H:%M:%SZ"),
})

# Sort by timestamp so the replay plays out in chronological order
rows.sort(key=lambda r: r["timestamp"])

out_path = "sample-data/transactions.csv"
with open(out_path, "w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=[
        "accountId", "amount", "currency", "merchant", "category", "location", "timestamp"
    ])
    writer.writeheader()
    writer.writerows(rows)

print(f"Wrote {len(rows)} transactions to {out_path}")

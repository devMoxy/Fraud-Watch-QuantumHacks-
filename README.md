# FraudWatch

Real-time transaction anomaly detection for fintech — built for QuantumHacks 2026.

FraudWatch ingests a stream of financial transactions, scores each one against
the *account's own recent history* using a mix of statistical and rule-based
signals, and surfaces flagged activity on a live dashboard as it happens.

## What it does

- **Ingests** transactions from CSV (bulk load or a paced live "replay" for demos).
- **Scores** every transaction in real time against four signals:
  - **Z-score outlier** — amount is a statistical outlier vs. the account's rolling mean/stddev.
  - **Large amount deviation** — amount is a large multiple of the rolling mean (used when an account has too little history for a stable stddev).
  - **High velocity** — too many transactions for one account in a short window (card-testing / bot pattern).
  - **New location** — first-ever transaction from this account at a given location.
- **Streams** every transaction and its verdict to the frontend over WebSocket, so the dashboard updates live as data comes in — no polling, no refresh.
- **Visualizes** flag rate, flags-by-signal, and a full audit table of what got flagged and why.

## Architecture

```
sample-data/transactions.csv
        │
        ▼
 backend (Spring Boot)
   ├─ CsvTransactionParser        parses uploaded CSV
   ├─ TransactionIngestionService bulk-load or paced "replay"
   ├─ AnomalyDetectionService     z-score / velocity / new-location checks
   ├─ Postgres (or H2 in dev)     stores transactions + anomaly flags
   ├─ REST API                   /api/transactions/*, /api/dashboard/*
   └─ WebSocket  /ws/transactions   broadcasts each scored transaction live
        │
        ▼
 frontend (React + Vite + Tailwind + Recharts)
   └─ live ticker feed, risk breakdown chart, flagged transactions table
```

## Running it locally

### 1. Start Postgres

```bash
docker compose up -d
```

This starts Postgres on `localhost:5432` with db `fraudwatch` / user `fraudwatch` / password `fraudwatch`.

> Don't want Docker? Skip this step — the backend defaults to an in-memory H2 database (profile `dev`), so it runs with zero setup. Use the `postgres` profile (see below) once you want persistence.

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run                      # uses in-memory H2 by default

# or, to use the Postgres container from step 1:
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Backend comes up on `http://localhost:8080`.

> No `mvnw` wrapper committed? Run `mvn -N io.takari:maven:wrapper` once, or just use a local `mvn` install — either works.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend comes up on `http://localhost:5173` and talks to the backend at `http://localhost:8080` (override with `VITE_API_BASE` — see `.env.example`).

### 4. Generate demo data (already included)

`sample-data/transactions.csv` is pre-generated with realistic per-account
spending patterns plus four seeded anomalies (an amount outlier, a new-location
hit, a velocity burst, and an early large-deviation spike). Regenerate it any time:

```bash
python3 scripts/generate_sample_data.py
```

### 5. Demo flow

1. Open the dashboard at `http://localhost:5173`.
2. Click **Choose CSV** → select `sample-data/transactions.csv`.
3. Click **Seed history** once (loads the bulk of transactions instantly, so accounts have a baseline).
4. Click **Choose CSV** again, pick the same file, and click **Replay transactions** — watch the ticker stream in live and flags light up in real time.

## API reference

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/transactions/ingest` | Bulk-load a CSV instantly, no delay |
| POST | `/api/transactions/replay?delayMs=400` | Paced replay, broadcasts each txn over WebSocket |
| GET | `/api/transactions/replay/status` | Whether a replay is currently running |
| GET | `/api/dashboard/recent` | Last 100 transactions with verdicts |
| GET | `/api/dashboard/flags` | Last 100 flagged transactions |
| GET | `/api/dashboard/stats` | Totals, flag rate, flags-by-signal |
| WS | `/ws/transactions` | Live feed of scored transactions |

## Technologies used

- **Backend:** Java 21, Spring Boot 3 (Web, WebSocket, Data JPA), Postgres / H2, Apache Commons CSV
- **Frontend:** React 18, Vite, Tailwind CSS, Recharts
- **Infra:** Docker Compose (Postgres)

## Problem & impact

Card-testing bots, account takeovers, and one-off large fraudulent purchases
usually look nothing like a customer's normal spending — but by the time a bank's
batch fraud jobs run, the damage is often done. FraudWatch demonstrates a
lightweight, explainable, real-time layer that scores every transaction the
instant it lands, using only the account's own history — no black-box model,
no labeled fraud dataset required, and every flag comes with a plain-English
reason an analyst (or a judge) can immediately understand.

## Team

- _Add your names here_

## License

MIT

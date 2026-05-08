

#  Incident Management System (IMS)

A mission-critical, production-grade Incident Management System built to monitor a distributed stack — APIs, MCP Hosts, Distributed Caches, Async Queues, RDBMS, and NoSQL stores — and manage the full failure mediation workflow from signal ingestion to closed RCA.

---
## Live Demo:
Backend: https://incident-managmenet-system1.onrender.com/
Frontend: https://incident-managmenet-systemfrontend.onrender.com/

## Table of Contents

- [Architecture Diagram](#architecture-diagram)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Backpressure Handling](#backpressure-handling)
- [Design Patterns](#design-patterns)
- [Quick Start — Docker Compose](#quick-start--docker-compose)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Sample Failure Simulation](#sample-failure-simulation)
- [Project Structure](#project-structure)
- [Bonus Additions](#bonus-additions)

---

## Architecture Diagram

```
                         ┌─────────────────────────────────────────────────────┐
                         │                  SIGNAL PRODUCERS                   │
                         │   (APIs / MCP Hosts / Queues / Cache / RDBMS)       │
                         └────────────────────────┬────────────────────────────┘
                                                  │  HTTP POST /api/signals
                                                  ▼
                         ┌─────────────────────────────────────────────────────┐
                         │             SPRING BOOT BACKEND (Port 8080)         │
                         │                                                     │
                         │  ┌─────────────┐   ┌──────────────────────────┐   │
                         │  │ Rate Limiter│   │  In-Memory Ring Buffer   │   │
                         │  │ (Bucket4j)  │──▶│  (10,000 signals/sec)    │   │
                         │  └─────────────┘   └──────────┬───────────────┘   │
                         │                               │                    │
                         │              ┌────────────────▼──────────────┐    │
                         │              │   Debounce Engine              │    │
                         │              │   (100 signals / 10s window    │    │
                         │              │    → 1 Work Item per component)│    │
                         │              └──────┬──────────────┬──────────┘    │
                         │                     │              │               │
                         │           ┌─────────▼──┐    ┌─────▼──────────┐   │
                         │           │  Alerting  │    │  Work Item     │   │
                         │           │  Strategy  │    │  State Machine │   │
                         │           │  (P0→P3)   │    │  OPEN→CLOSED   │   │
                         │           └────────────┘    └────────────────┘   │
                         └───────┬────────────┬───────────────┬──────────────┘
                                 │            │               │
               ┌─────────────────▼──┐  ┌─────▼──────┐  ┌────▼──────────────┐
               │  TimescaleDB       │  │  MongoDB   │  │  Redis             │
               │  (PostgreSQL 15)   │  │  (v7)      │  │  (v7-alpine)       │
               │                   │  │            │  │                    │
               │  • Work Items      │  │ • Raw      │  │ • Dashboard State  │
               │  • RCA Records     │  │   Signal   │  │ • Debounce Windows │
               │  • Status History  │  │   Payloads │  │ • Hot-path cache   │
               │  • Transactional   │  │ • Audit Log│  │                    │
               │  • Timeseries aggs │  │            │  │                    │
               └────────────────────┘  └────────────┘  └────────────────────┘

                         ┌─────────────────────────────────────────────────────┐
                         │             REACT FRONTEND (Port 3000)              │
                         │                                                     │
                         │   Dashboard  │  Incident Detail  │  RCA Form        │
                         │   (live feed)│  (signals + flow) │  (mandatory)     │
                         │                                                     │
                         │   Auto-refreshes every 5s via polling               │
                         └─────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3 |
| Primary DB | PostgreSQL 15 via TimescaleDB (transactional + timeseries) |
| NoSQL / Audit | MongoDB 7 (raw signal payloads) |
| Cache | Redis 7 (dashboard hot-path, debounce windows) |
| Frontend | React 18, TypeScript, React Router v6 |
| Containerisation | Docker, Docker Compose |
| Rate Limiting | Bucket4j (token-bucket algorithm) |

---

## Features

### Backend Engine
- **High-throughput signal ingestion** — handles bursts up to 10,000 signals/sec via an in-memory ring buffer; the persistence layer never blocks ingestion
- **Debounce logic** — 100 signals for the same `componentId` within a 10-second window collapses into a single Work Item; all 100 raw signals remain linked in MongoDB
- **Alerting Strategy pattern** — component type drives priority: RDBMS failure → P0, API degradation → P1, Cache miss → P2, Queue lag → P3
- **State machine** — Work Items follow a strict `OPEN → INVESTIGATING → RESOLVED → CLOSED` lifecycle; invalid transitions are rejected
- **Mandatory RCA** — the system hard-blocks any `CLOSED` transition if an RCA object is absent or incomplete
- **MTTR calculation** — automatically computed from `start_time` (first signal received) to `end_time` (RCA submission timestamp)
- **Async processing** — all signal persistence and alerting run on a dedicated async thread pool; HTTP ingestion never waits for DB writes
- **Rate limiting** — token-bucket rate limiter on `/api/signals` prevents cascading overload
- **Observability** — `/health` endpoint + throughput metrics (signals/sec) logged to console every 5 seconds

### Frontend
- **Live incident feed** — auto-refreshes every 5 seconds, sorted by severity (P0 first)
- **Priority summary cards** — real-time count of active incidents per priority level
- **Incident detail view** — full signal feed from MongoDB + status timeline
- **RCA form** — date-time pickers, root cause category dropdown, fix applied and prevention steps text areas
- **Status progression** — one-click workflow buttons that enforce the state machine

---

## Backpressure Handling

This is one of the core resilience requirements of the system. The problem: signals arrive at up to 10,000/sec, but database writes (PostgreSQL, MongoDB) are orders of magnitude slower.

### Strategy: In-Memory Ring Buffer + Async Drain

```
Signal HTTP Request
       │
       ▼
 ┌─────────────────────────┐
 │   Rate Limiter          │  ← Bucket4j: rejects excess at the edge
 └────────────┬────────────┘
              │
              ▼
 ┌─────────────────────────┐
 │   In-Memory Ring Buffer │  ← LinkedBlockingQueue (bounded, e.g. 100k slots)
 │   (non-blocking offer)  │    If full → HTTP 429 back to producer
 └────────────┬────────────┘
              │  drained by async worker pool
              ▼
 ┌─────────────────────────┐
 │   Debounce Engine       │  ← Redis-backed 10s sliding window per componentId
 └────────────┬────────────┘
              │
     ┌────────┴────────┐
     ▼                 ▼
  MongoDB           PostgreSQL
  (raw signals)    (work items)
```

**Key decisions:**

1. **Bounded queue with non-blocking offer** — `offer()` instead of `put()` means the HTTP thread is never blocked. If the queue is full, the signal is dropped and a `429 Too Many Requests` is returned immediately. The system never crashes waiting on a slow DB.

2. **Bucket4j token-bucket rate limiter** — applied before the queue. This is the first line of defence, shaping traffic before it even touches the buffer.

3. **Redis debounce window** — a Redis key per `componentId` with a 10-second TTL acts as the deduplication gate. The first signal creates a Work Item in PostgreSQL; subsequent signals within the window are written only to MongoDB and linked to the existing Work Item. This dramatically reduces write pressure on the transactional store during an outage storm.

4. **Async thread pool for persistence** — Spring's `@Async` with a dedicated `ThreadPoolTaskExecutor` decouples HTTP ingestion from database I/O entirely. The ingestion endpoint returns `202 Accepted` immediately.

5. **TimescaleDB for timeseries aggregations** — raw signal volume over time is queryable without table scans, keeping the dashboard responsive even with millions of rows.

---

## Design Patterns

| Pattern | Where Used | Why |
|---|---|---|
| **Strategy** | Alerting (`AlertingStrategy` interface) | Swap P0/P1/P2/P3 alert logic per component type without changing the core engine |
| **State Machine** | Work Item lifecycle | Enforces valid transitions, rejects illegal ones (e.g. OPEN → CLOSED without RCA) |
| **Repository** | Data access layer | Abstracts PostgreSQL, MongoDB, Redis behind clean interfaces |
| **Builder** | Work Item / RCA construction | Readable object construction with validation |
| **Observer / Event** | Async signal processing | Spring application events decouple ingestion from persistence |

---

## Quick Start — Docker Compose

### Prerequisites
- Docker 24+ and Docker Compose v2
- Java 17 (only needed if running backend outside Docker)
- Node.js 18+ (only needed if running frontend outside Docker)

### 1. Clone the repository

```bash
git clone https://github.com/DonaBiswas45/incident-managmenet-system.git
cd incident-managmenet-system
```

### 2. Start all infrastructure services

```bash
docker compose up -d
```

This starts:
- **TimescaleDB** (PostgreSQL 15) on port `5432`
- **MongoDB 7** on port `27017`
- **Redis 7** on port `6379`

Wait ~10 seconds for all services to be healthy.

### 3. Start the backend

```bash
cd backend/backend
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`

### 4. Start the frontend

```bash
cd frontend
npm install
npm start
```

Frontend runs on `http://localhost:3000`

### 5. Verify everything is running

```bash
curl http://localhost:8080/health
# Expected: {"status":"UP", ...}
```

---

## Environment Variables

The backend reads these from `application.properties` (or environment overrides):

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/dona_db` | TimescaleDB connection |
| `SPRING_DATASOURCE_USERNAME` | `dona_user` | PostgreSQL user |
| `SPRING_DATASOURCE_PASSWORD` | `dona_pass` | PostgreSQL password |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/ims` | MongoDB connection |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `PORT` | `8080` | Server port |

---

## API Reference

### Signal Ingestion

```
POST /api/signals
Content-Type: application/json

{
  "componentId": "DB_CLUSTER_01",
  "componentType": "RDBMS",
  "severity": "critical",
  "errorCode": "CONNECTION_REFUSED",
  "errorMessage": "Primary node unreachable after 3 retries"
}

Response: 202 Accepted
```

### Work Items

```
GET  /api/work-items              → list all work items
GET  /api/work-items/:id          → get single work item
GET  /api/work-items/:id/signals  → raw signals from MongoDB
GET  /api/work-items/:id/history  → status transition history
PUT  /api/work-items/:id/status   → advance workflow state
```

### RCA

```
POST /api/work-items/:id/rca      → submit RCA (required before CLOSED)
GET  /api/work-items/:id/rca      → fetch submitted RCA
```

### Observability

```
GET  /health                      → Spring Actuator health check
```

---

## Sample Failure Simulation

A simulation script is provided to mock a real-world cascade: an RDBMS outage triggers 150 signals, followed by an MCP Host failure.

### Run the simulation

```bash
cd backend/backend/scripts
chmod +x simulate_failure.sh
./simulate_failure.sh
```

Or use the JSON payload directly:

```bash
# Step 1: Simulate RDBMS outage (fires 150 signals to trigger debounce)
for i in $(seq 1 150); do
  curl -s -X POST http://localhost:8080/api/signals \
    -H "Content-Type: application/json" \
    -d '{
      "componentId": "DB_CLUSTER_01",
      "componentType": "RDBMS",
      "severity": "critical",
      "errorCode": "PRIMARY_NODE_DOWN",
      "errorMessage": "Primary PostgreSQL node unreachable — connection pool exhausted"
    }' &
done
wait
echo "✅ RDBMS outage simulated — check dashboard for P0 work item"

# Step 2: Simulate MCP Host failure (separate component, new work item)
sleep 2
curl -X POST http://localhost:8080/api/signals \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "MCP_HOST_01",
    "componentType": "MCP_HOST",
    "severity": "error",
    "errorCode": "HEALTH_CHECK_FAILED",
    "errorMessage": "MCP host failed 3 consecutive health checks — possible cascading failure from DB_CLUSTER_01"
  }'
echo "✅ MCP Host failure simulated — check dashboard for P1 work item"
```

**Expected result:**
- Dashboard shows 1 × P0 (RDBMS) + 1 × P1 (MCP Host)
- P0 work item has `signalCount: 150` (all 150 raw signals in MongoDB, debounced to 1 work item in PostgreSQL)
- Console logs show signals/sec throughput metric

### Sample JSON payloads — `scripts/mock_events.json`

```json
[
  {
    "scenario": "RDBMS Primary Failure",
    "componentId": "DB_CLUSTER_01",
    "componentType": "RDBMS",
    "severity": "critical",
    "errorCode": "PRIMARY_NODE_DOWN",
    "errorMessage": "Primary PostgreSQL node unreachable after 3 retries",
    "expectedPriority": "P0"
  },
  {
    "scenario": "MCP Host Cascade",
    "componentId": "MCP_HOST_01",
    "componentType": "MCP_HOST",
    "severity": "error",
    "errorCode": "HEALTH_CHECK_FAILED",
    "errorMessage": "MCP host heartbeat timeout — 15s with no response",
    "expectedPriority": "P1"
  },
  {
    "scenario": "Cache Cluster Latency",
    "componentId": "CACHE_CLUSTER_01",
    "componentType": "CACHE",
    "severity": "warning",
    "errorCode": "LATENCY_SPIKE",
    "errorMessage": "Redis p99 latency exceeded 200ms threshold",
    "expectedPriority": "P2"
  },
  {
    "scenario": "Async Queue Lag",
    "componentId": "QUEUE_WORKER_01",
    "componentType": "ASYNC_QUEUE",
    "severity": "warning",
    "errorCode": "CONSUMER_LAG",
    "errorMessage": "Queue depth exceeded 50,000 messages — consumer throughput degraded",
    "expectedPriority": "P3"
  }
]
```

---

## Project Structure

```
incident-managmenet-system/
├── docker-compose.yml              # TimescaleDB + MongoDB + Redis
├── README.md
│
├── backend/
│   └── backend/
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
│           └── main/
│               ├── java/com/ims/backend/
│               │   ├── controller/         # REST endpoints
│               │   ├── service/            # Business logic, async processing
│               │   ├── strategy/           # Alerting Strategy pattern (P0-P3)
│               │   ├── statemachine/       # Work Item state transitions
│               │   ├── repository/         # PostgreSQL + MongoDB repos
│               │   ├── model/              # Work Item, Signal, RCA entities
│               │   └── config/             # Redis, async executor, CORS, rate limiter
│               └── resources/
│                   ├── application.properties
│                   └── application-prod.properties
│
├── frontend/
│   ├── public/
│   └── src/
│       ├── pages/
│       │   ├── Dashboard.tsx       # Live incident feed
│       │   ├── IncidentDetail.tsx  # Signal feed + status timeline
│       │   └── RcaForm.tsx         # Mandatory RCA submission
│       ├── api.ts                  # Axios API client
│       ├── types.ts                # TypeScript interfaces
│       └── App.tsx                 # Router
│
└── scripts/
    ├── simulate_failure.sh         # Bash simulation script
    └── mock_events.json            # Sample failure payloads
```

---

## Bonus Additions

- **TimescaleDB** instead of plain PostgreSQL — enables native time-series aggregations on signal volume (e.g. signals/min per component) without extra infrastructure
- **Real-time throughput metrics** — the backend prints `[METRICS] Signals ingested: X/sec` to console every 5 seconds via a scheduled task, providing immediate observability without a full APM setup
- **Priority auto-classification** — component type is mapped to priority automatically via the Strategy pattern, so engineers don't need to manually triage severity on every incoming signal
- **MTTR auto-calculation** — mean time to repair is computed and stored on RCA submission, enabling post-incident analytics without manual spreadsheets
- **Staggered UI animations** — incident rows fade in with staggered delays so engineers can visually track new incidents appearing in real time
- **Signal count highlighting** — signal counts above 50 are highlighted amber in the dashboard, giving a quick visual cue for high-volume incidents before an engineer opens the detail view

---

## Submission Checklist

- [x] `/backend` — Spring Boot 3, Java 17
- [x] `/frontend` — React 18, TypeScript
- [x] `docker-compose.yml` — one-command local setup
- [x] Architecture diagram (see above)
- [x] Backpressure section with strategy explanation
- [x] Sample failure simulation script + JSON payloads
- [x] Design patterns documented
- [x] `/health` endpoint
- [x] Mandatory RCA enforcement
- [x] MTTR auto-calculation
- [x] Debounce logic (100 signals / 10s → 1 Work Item)

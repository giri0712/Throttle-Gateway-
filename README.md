# ThrottleGate

**Distributed API Rate Limiting & Traffic Control Platform**

A standalone rate-limiting microservice that any backend can plug into. ThrottleGate makes allow/deny decisions for your API traffic in real time — backed by Redis/Valkey with atomic Lua scripts so it stays correct across multiple service instances — and ships with a live dashboard showing traffic being throttled as it happens.

```
┌──────────────┐      ┌────────────────────────────┐      ┌───────────────────┐
│   Clients    │─────▶│     ThrottleGate API        │─────▶│  Redis / Valkey   │
│  (any app)   │      │  GET /v1/check  ───────────▶│      │  (distributed     │
│              │◀─────│  allow / deny + Retry-After │◀─────│   state + Lua)    │
└──────────────┘      └───────────┬────────────────┘      └───────────────────┘
                                  │ metrics
                                  ▼
                        ┌────────────────────┐
                        │   Admin Dashboard   │   React, live charts
                        │  (React + chart.js) │
                        └────────────────────┘
```

## Key Features

- **Synchronous decision API** — `GET /v1/check` returns allow/deny instantly with a `Retry-After` header on rejection
- **Three pluggable algorithms** — token bucket, sliding window log, sliding window counter (strategy pattern, switch via config)
- **Distributed & race-free** — all algorithms run as atomic Lua scripts in Redis/Valkey; no race conditions across instances
- **Tier-based limits** — per-client, per-endpoint, per-tier (free/pro) limits with a sensible default fallback
- **Live admin dashboard** — requests/sec, allowed/denied counts, allow ratio, and throughput charts auto-refreshing every 5s
- **Observability** — Spring Boot Actuator (health, metrics, Prometheus) + Micrometer counters
- **Drop-in integration** — a Spring Boot starter exposes a one-line `ThrottleGateClient` for other services

## Modules

| Module | Description |
|--------|-------------|
| [`throttle-gate/`](throttle-gate/) | Core rate-limiting service (Java 17 + Spring Boot 3). Decision API, algorithms, metrics, CORS/OpenAPI config. |
| [`throttle-gate-dashboard/`](throttle-gate-dashboard/) | Real-time admin dashboard (React 18 + Tailwind CSS + chart.js). |
| [`throttle-gate-spring-boot-starter/`](throttle-gate-spring-boot-starter/) | Spring Boot starter exposing a `ThrottleGateClient` bean for downstream services. |

## Getting Started

### Prerequisites

- Java 17+, Maven 3.8+ (to run the service from source)
- Node.js 14+ (to run the dashboard)
- Redis or Valkey (≥ 6) and PostgreSQL — or just use Docker Compose

### Option A — Docker Compose (easiest)

```bash
docker compose up --build
```

This starts all three dependencies + the service:

| Service | URL |
|---|---|
| ThrottleGate API | `http://localhost:8080` |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |

### Option B — Run from source

```bash
# 1. Start Redis/Valkey and PostgreSQL (or point application.yml at existing ones)

# 2. Start the service
cd throttle-gate
mvn spring-boot:run

# 3. In another terminal, start the dashboard
cd throttle-gate-dashboard
npm install
npm start
```

Open the dashboard at **http://localhost:3000**. It polls `http://localhost:8080/api/metrics/throttlegate.requests` by default — point it elsewhere with `REACT_APP_API_URL`:

```bash
REACT_APP_API_URL=http://my-host:8080 npm start
```

> **Note:** the Spring Cloud Config client is **disabled by default** in `application.yml` — this project does not ship a config server, so no extra setup is needed. It can be re-enabled if you deploy one (see the comments in `application.yml`).

## API Reference

### Rate limit check

```
GET /v1/check?clientId={clientId}&endpoint={endpoint}&tier={tier}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `clientId` | string | ✅ | Identifier for the calling client |
| `endpoint` | string | ✅ | API endpoint being accessed (e.g. `/events`) |
| `tier` | string | ❌ | `free` (default) or `pro` |

**Allowed** — `200 OK`:

```json
{
  "allowed": true,
  "clientId": "client123",
  "endpoint": "/events",
  "tier": "free"
}
```

**Denied** — `429 Too Many Requests` with a `Retry-After` header (seconds to wait):

```bash
curl -i "http://localhost:8080/v1/check?clientId=demo&endpoint=/payments&tier=free"

HTTP/1.1 429
Retry-After: 60
{
  "allowed": false,
  "clientId": "demo",
  "endpoint": "/payments",
  "tier": "free"
}
```

### Metrics (used by the dashboard)

```
GET /api/metrics/throttlegate.requests
```

```json
{
  "requestsPerSecond": 12.5,
  "allowedCount": 1234,
  "deniedCount": 56,
  "allowRatio": 95.66
}
```

### Actuator endpoints

`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`, `/actuator/refresh` (config hot-reload).

## Rate Limiting Algorithms

All three algorithms run **atomically in Redis/Valkey via Lua scripts**, so concurrent requests across any number of instances are safe.

| Algorithm | Config value | When to use |
|-----------|-------------|-------------|
| Token Bucket | `token-bucket` *(default)* | Steady, burst-tolerant traffic |
| Sliding Window Log | `sliding-window-log` | Burst-sensitive endpoints needing exact counts |
| Sliding Window Counter | `sliding-window-counter` | Memory-efficient approximation of the sliding window |

Each decision is keyed by `clientId:endpoint:tier` and enforces the configured limit within the configured window. Keys expire automatically via TTL.

## Configuration

All settings live in `throttle-gate/src/main/resources/application.yml` under the `throttlegate` prefix:

```yaml
throttlegate:
  algorithm: token-bucket              # token-bucket | sliding-window-log | sliding-window-counter
  window-size-seconds: 60              # length of the rate-limit window
  default-limits:                      # tier:endpoint -> requests per window
    "[free:/events]": 100
    "[free:/payments]": 10
    "[free:/default]": 50
    "[pro:/events]": 1000
    "[pro:/payments]": 100
    "[pro:/default]": 500
  cors:
    allowed-origins: http://localhost:3000
```

| Property | Default | Description |
|----------|---------|-------------|
| `throttlegate.algorithm` | `token-bucket` | Active rate limiting algorithm |
| `throttlegate.window-size-seconds` | `60` | Time window for limits |
| `throttlegate.default-limits` | — | Map of `tier:endpoint` → request limit (YAML only) |
| `throttlegate.cors.allowed-origins` | — | Comma-separated origins allowed for the dashboard |
| `spring.redis.host` / `spring.redis.port` | `localhost:6379` | Redis/Valkey connection |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/throttlegate` | PostgreSQL connection |

Environment variables override everything in YAML, e.g. `SPRING_REDIS_HOST`, `SPRING_DATASOURCE_URL`, `THROTTLEGATE_CORS_ALLOWED_ORIGINS` — useful for containers.

## Integrating Other Services

Add the starter dependency to any Spring Boot app:

```xml
<dependency>
    <groupId>com.throttlegate</groupId>
    <artifactId>throttle-gate-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```properties
throttlegate.service-url=http://localhost:8080
throttlegate.timeout=5
```

```java
@Autowired
private ThrottleGateClient throttleGateClient;

if (throttleGateClient.isAllowed("client123", "/events", "free")) {
    // process request
} else {
    // handle 429 — client is rate limited
}
```

See [`throttle-gate-spring-boot-starter/README.md`](throttle-gate-spring-boot-starter/README.md) for the full API.

## Monitoring & Observability

- **Dashboard** — real-time UI at `http://localhost:3000` (requests/sec, allow ratio, throughput chart, allow vs. deny breakdown)
- **Prometheus** — `GET /actuator/prometheus` exposes Micrometer metrics (`throttlegate.requests.allowed`, `throttlegate.requests.denied`, `throttlegate.requests.per_second`, ...)
- **Actuator** — health and metrics under `/actuator/*`

## Repository Layout

```
├── throttle-gate/                     # Core service (Spring Boot)
│   ├── src/main/java/com/throttlegate/
│   │   ├── controller/                # REST endpoints (/v1/check, /api/metrics)
│   │   ├── ratelimiter/               # Strategy interface + 3 Lua-backed algorithms
│   │   ├── metrics/                   # Micrometer counters + RPS calculation
│   │   └── config/                    # Redis, CORS, OpenAPI, rate-limit settings
│   └── Dockerfile
├── throttle-gate-dashboard/           # Admin dashboard (React + Tailwind + chart.js)
│   └── src/components/                # MetricCard, RpsLineChart, AllowDenyChart
├── throttle-gate-spring-boot-starter/ # Integration library for downstream services
├── docs/
│   └── AWS_DEPLOYMENT_PLAN.md         # Step-by-step AWS Free Tier deployment plan
└── docker-compose.yml
```

## Deployment

For a step-by-step AWS Free Tier deployment (EC2 + Valkey + PostgreSQL + nginx + CloudWatch), see **[`docs/AWS_DEPLOYMENT_PLAN.md`](docs/AWS_DEPLOYMENT_PLAN.md)**.

## Documentation

- Service architecture & internals — [`throttle-gate/README.md`](throttle-gate/README.md)
- Dashboard setup & customization — [`throttle-gate-dashboard/README.md`](throttle-gate-dashboard/README.md)
- Spring Boot starter — [`throttle-gate-spring-boot-starter/README.md`](throttle-gate-spring-boot-starter/README.md)

## License

MIT

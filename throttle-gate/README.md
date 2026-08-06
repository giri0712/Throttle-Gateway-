# ThrottleGate

Distributed API Rate Limiting & Traffic Control Platform

A standalone rate-limiting microservice that any backend can plug into, with a live dashboard showing traffic being throttled in real time.

## Architecture

### Core Components

1. **Core Rate Limiter Service (Java + Spring Boot)**
   - `/v1/check` — synchronous decision endpoint (allow/deny + retry-after)
   - Three pluggable algorithms via strategy pattern:
     - Token Bucket
     - Sliding Window Log
     - Sliding Window Counter
   - Per-client, per-endpoint, per-tier (free/pro) limit configurations
   - Config hot-reload via Spring Cloud Config

2. **Distributed State Layer (Valkey on EC2 t3.micro)**
   - Lua scripts for atomic check-and-decrement (prevents race conditions)
   - TTL-based key expiry so old windows self-clean

3. **Integration Layer**
   - Spring Boot filter/interceptor library (publish as a small jar)
   - Can be dropped into other projects as middleware

4. **Admin + Observability Dashboard**
   - Live requests/sec, allow vs deny ratio, per-client breakdown
   - CloudWatch custom metrics + alarms

5. **Documentation**
   - ADR: algorithm selection rationale
   - Architecture diagram
   - README with live demo GIF

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL
- Redis/Valkey

### Running the Service

1. Clone the repository
2. Configure database and Redis in `src/main/resources/application.properties`
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### API Usage

Check if a request is allowed:
```
GET /v1/check?clientId=client123&endpoint=/events&tier=free
```

Response:
```json
{
  "allowed": true,
  "clientId": "client123",
  "endpoint": "/events",
  "tier": "free"
}
```

Headers:
- `Retry-After`: seconds to wait before retrying (when denied)

### Configuration

Rate limiting algorithm can be configured via:
```
throttlegate.algorithm=token-bucket
```

Options: `token-bucket`, `sliding-window-log`, `sliding-window-counter`

## Architecture Decision Records (ADR)

See `docs/adr/` for detailed decisions on:
- Why token bucket for steady APIs vs sliding window log for burst-sensitive endpoints
- Choice of Valkey/Redis for distributed state
- Hot-reload configuration approach

## Load Testing

See `docs/load-testing/` for k6 scripts and results.

## AWS Free Tier Usage

- EC2 t3.micro (750 hrs/mo) — Valkey + app if needed
- RDS free tier (db.t3.micro, 750 hrs/mo, 20GB) — Postgres for configs
- CloudWatch — metrics/alarms (always-free tier limits)
- S3 (5GB) — static dashboard hosting
- Route53 optional (~$0.50/mo)

## Implementation Notes

### Race Condition Prevention
The implementation uses Lua scripts in Redis/Valkey for atomic check-and-decrement operations to prevent race conditions that could occur with separate GET and SET operations.

### Fail-Open vs Fail-Closed
When the backing store (Valkey/Redis) is unavailable, the service defaults to fail-open (allow requests) to prevent causing a denial of service for legitimate traffic. This behavior can be configured.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a pull request

## License

MIT License
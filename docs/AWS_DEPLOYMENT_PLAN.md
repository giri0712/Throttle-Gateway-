# ThrottleGate — AWS Free Tier Deployment Plan

Implementation plan for deploying ThrottleGate to AWS on the Free Tier. Follow the phases in order; each phase is independently verifiable.

---

## 0. Goal & Recommended Architecture

**Goal:** Run the ThrottleGate rate limiting service + admin dashboard on AWS Free Tier, with monitoring, at (near) zero cost.

**Recommended topology (single instance, minimal cost):**

```
Internet
   │
   ├── :80 / :443  →  nginx → Dashboard (static React build served locally)
   ├── :8080       →  ThrottleGate Spring Boot app (API)
   │                   ├── Valkey (Redis fork) — same instance, localhost
   │                   └── PostgreSQL — same instance, localhost
   └── :22         →  SSH (admin only)
```

- **EC2 `t3.micro`** (1 vCPU? no — 2 vCPU / 1 GB RAM): app + Valkey + Postgres + nginx
- **Skip RDS** — self-host Postgres; it's a config store, not the core of the app (saves ~$14/mo and one service to manage)
- **CloudWatch** — billing alarm (mandatory) + optional app alarms
- **Dashboard** — serve the built React app from nginx on the same instance (simplest); S3 static hosting is the fallback if you want it separate

> Decision to make at implementation time: Terraform (infra-as-code) vs. AWS Console (point-and-click). This plan gives both; Terraform is recommended if you'll redeploy more than once.

---

## 1. Free Tier Reality Check (read this before anything else)

The program changed **July 15, 2025**. Two possible situations:

| | Old account (pre-Jul 2025) | New account (2025+) |
|---|---|---|
| Free resources | EC2 t3.micro 750 h/mo, RDS db.t3.micro, S3 5 GB — **12 months** | **$200 credits** ($100 signup + $100 onboarding), 30+ always-free services |
| Free plan duration | 12 months | **6 months** (then account closes unless you upgrade to paid) |
| EC2/RDS instances free? | Yes (within limits) | **No** — paid from the start, credits offset the bill |

**Consequences for this plan:**
- New account: a t3.micro (~$8/mo) + egress will drain the $200 credits over ~6–12 months of light usage. Acceptable for a demo; do NOT leave it running 24/7 indefinitely.
- **Set the billing alarm in Phase 2 — it is not optional.**
- Stop the instance when not demoing (stopped EC2 = no compute charge; storage still costs pennies).

---

## 2. Phase 0 — Account & Budget Setup (10 min)

1. Sign in to AWS Console → **Billing**.
2. Enable **Cost Explorer** and create a **monthly budget** (e.g., $10) with email alerts at 50/80/100%.
3. (New accounts) Find the **Credits page** — note credit balance and expiration; set a calendar reminder 1 month before expiry.
4. Choose a region: `us-east-1` (cheapest, most free-tier coverage).
5. Create/download a **key pair** (`throttlegate-key.pem`) — you'll need it for SSH.

---

## 3. Phase 1 — Prepare the App for Production (local, 20 min)

### 3.1 Required app changes before deploying

- [x] **Done already** — config moved to `application.yml`; app boots with `SPRING_CLOUD_CONFIG_ENABLED=false` (config server does not exist; do not enable it on EC2).
- [x] **Done already** — rate limiter Lua scripts fixed; Redis serializer args fixed; `/v1/check` and `/api/metrics` verified working locally.
- [ ] **To do:** make the dashboard API URL build-configurable so `npm run build` points at the EC2 host:
  ```bash
  REACT_APP_API_URL=http://<EC2-PUBLIC-IP> npm run build
  ```
  (`src/App.js` already reads `process.env.REACT_APP_API_URL`.)
- [ ] **To do (optional):** set JVM heap explicitly for 1 GB RAM:
  `-Xmx256m -Xms128m -XX:+UseSerialGC` (see Phase 4 systemd unit).

### 3.2 Env vars the app needs on EC2

| Env var | Value | Notes |
|---|---|---|
| `SPRING_CLOUD_CONFIG_ENABLED` | `false` | **Critical — app won't boot without it** |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/throttlegate` | |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | |
| `SPRING_DATASOURCE_PASSWORD` | `<pick-one>` | |
| `SPRING_REDIS_HOST` | `localhost` | |
| `SPRING_REDIS_PORT` | `6379` | |
| `THROTTLEGATE_CORS_ALLOWED_ORIGINS` | dashboard origin(s) | e.g. `http://<EC2-IP>` or `https://your-domain` |
| `SERVER_PORT` | `8080` | default |

---

## 4. Phase 2 — Infrastructure (30–60 min)

### Option A: Terraform (recommended)

Resources to define in `infra/`:

1. **VPC + subnet** (default VPC is fine to start; custom VPC later)
2. **Security group** — this is your firewall; be strict:
   - `22` from your IP only (SSH)
   - `80`, `443` from `0.0.0.0/0` (nginx → dashboard)
   - `8080` from `0.0.0.0/0` **only if you want the raw API public**; otherwise restrict to your IP or use nginx as a reverse proxy for `/v1/check`
3. **`t3.micro` instance**, `amazon-linux-2023` AMI, 8 GB gp3 root volume, with **user-data script** (Phase 3) and the key pair
4. **Elastic IP** (static address so the dashboard URL doesn't break on restart) — free while the instance runs
5. **CloudWatch billing alarm**:
   ```hcl
   resource "aws_cloudwatch_metric_alarm" "billing" {
     alarm_name          = "throttlegate-billing"
     comparison_operator = "GreaterThanThreshold"
     evaluation_periods  = "1"
     metric_name         = "EstimatedCharges"
     namespace           = "AWS/Billing"
     period              = "21600"
     statistic           = "Maximum"
     threshold           = "10"
     alarm_actions       = [aws_sns_topic.alerts.arn]
   }
   ```
6. **SNS topic** → email subscription (this is how the billing alarm reaches you)

### Option B: AWS Console

1. EC2 → Launch instance → Amazon Linux 2023, `t3.micro`, key pair, 8 GB gp3.
2. Security group: as above (SSH your-IP, 80/443 anywhere, 8080 optional).
3. Allocate an Elastic IP and associate it.
4. Billing → Budgets → create monthly $10 budget with email alerts.
5. Run the user-data script from Phase 3 (or run it manually over SSH).

---

## 5. Phase 3 — Instance Bootstrap (user-data script)

Saves as `bootstrap.sh`; paste into the instance's **user data** (Terraform) or run over SSH (Console). Amazon Linux 2023:

```bash
#!/bin/bash
set -euxo pipefail

# --- Java 17 ---
dnf install -y java-17-amazon-corretto-devel

# --- PostgreSQL ---
dnf install -y postgresql15-server
postgresql-setup --initdb
systemctl enable --now postgresql
sudo -u postgres psql -c "CREATE USER throttlegate WITH PASSWORD 'change-me';"
sudo -u postgres psql -c "CREATE DATABASE throttlegate OWNER throttlegate;"

# --- Valkey (Redis fork, packaged on AL2023) ---
dnf install -y valkey
systemctl enable --now valkey

# --- nginx (serves the dashboard build) ---
dnf install -y nginx
systemctl enable nginx

# --- App directory + service user ---
useradd -r -m -d /opt/throttlegate throttlegate || true
mkdir -p /opt/throttlegate/app
```

**Verify after bootstrap:** `java -version`, `systemctl status postgresql valkey`, `redis-cli ping` → `PONG`, `sudo -u postgres psql -c "\l"` shows the `throttlegate` DB.

---

## 6. Phase 4 — Deploy the App (15 min)

1. Build locally:
   ```bash
   cd throttle-gate && mvn clean package -DskipTests
   ```
2. Copy to the instance:
   ```bash
   scp -i throttlegate-key.pem target/throttle-gate-1.0.0-SNAPSHOT.jar \
     ec2-user@<EC2-IP>:/opt/throttlegate/app/throttle-gate.jar
   ```
3. Create `/etc/systemd/system/throttlegate.service`:
   ```ini
   [Unit]
   Description=ThrottleGate rate limiting service
   After=network.target postgresql.service valkey.service

   [Service]
   User=throttlegate
   WorkingDirectory=/opt/throttlegate/app
   ExecStart=/usr/bin/java -Xmx256m -Xms128m -XX:+UseSerialGC -jar throttle-gate.jar
   Environment=SPRING_CLOUD_CONFIG_ENABLED=false
   Environment=SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/throttlegate
   Environment=SPRING_DATASOURCE_USERNAME=throttlegate
   Environment=SPRING_DATASOURCE_PASSWORD=change-me
   Environment=SPRING_REDIS_HOST=localhost
   Environment=SPRING_REDIS_PORT=6379
   Environment=THROTTLEGATE_CORS_ALLOWED_ORIGINS=http://<EC2-IP>
   Restart=always
   RestartSec=5

   [Install]
   WantedBy=multi-user.target
   ```
4. Enable + start:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now throttlegate
   sudo journalctl -u throttlegate -f   # watch the boot log
   ```

---

## 7. Phase 5 — Smoke Tests (what "it works" means)

```bash
# App is alive
curl -s http://localhost:8080/actuator/health            # {"status":"UP"}

# Rate limiting works (10-limit endpoint → 10x 200, then 429)
for i in $(seq 1 12); do
  curl -s -o /dev/null -w "%{http_code} " \
    "http://localhost:8080/v1/check?clientId=demo&endpoint=/payments"
done; echo

# Metrics feed the dashboard
curl -s http://localhost:8080/api/metrics/throttlegate.requests
# → {"requestsPerSecond":..., "allowedCount":..., "deniedCount":..., "allowRatio":...}

# Dashboard reachable (after Phase 6)
curl -s -I http://<EC2-IP>/
```

**Dashboard deployment (same instance, simplest path):**
```bash
cd throttle-gate-dashboard
REACT_APP_API_URL=http://<EC2-IP> npm run build
scp -r -i throttlegate-key.pem build/* ec2-user@<EC2-IP>:/usr/share/nginx/html/
```
Enable the default nginx `server` block (AL2023 ships one serving `/usr/share/nginx/html`). The dashboard polls the API every 5 s — the CORS header from the app allows the origin you set in `THROTTLEGATE_CORS_ALLOWED_ORIGINS`.

---

## 8. Phase 6 — Monitoring (CloudWatch)

The app exposes `GET /actuator/prometheus` (Micrometer) and `GET /api/metrics/throttlegate.requests` (dashboard JSON).

**Option A — CloudWatch agent (matches your docs' "CloudWatch" goal):**
1. Install the unified agent: `sudo dnf install -y amazon-cloudwatch-agent`
2. Configure `/opt/aws/amazon-cloudwatch-agent/bin/config.json` with `collectd`/statsd or a custom shell script scraping `/api/metrics/throttlegate.requests` into a custom metric.
3. Enable + start the agent; create a CloudWatch alarm on `throttlegate.denied` rising.

**Option B — Prometheus + Grafana on the instance (always-free, no vendor lock):** add `prometheus` + `grafana` RPMs, scrape `localhost:8080/actuator/prometheus`. Simpler to set up than the agent for custom metrics; costs nothing extra.

**Minimum viable monitoring (do at least this):** the billing alarm from Phase 2 + a CloudWatch alarm on the EC2 instance's `StatusCheckFailed` metric.

---

## 9. Phase 7 — Cost Control & Teardown

Rough monthly costs (us-east-1, on-demand, running 24/7):

| Item | Cost |
|---|---|
| EC2 t3.micro (Linux) | ~$7.50 |
| 8 GB gp3 + snapshot | ~$1 |
| Elastic IP (instance running) | $0 |
| Postgres/Valkey/nginx (self-hosted) | $0 |
| Data egress (light dashboard traffic) | ~$0–2 |
| **Total** | **~$8.50–10/mo** (offset by credits on new accounts) |

**Rules to follow:**
- Stop the instance when not demoing → saves ~$7.50/mo.
- Never enable RDS "just because the docs said so" — self-hosted Postgres is free and sufficient.
- When done with the project: **terminate the instance, release the Elastic IP, delete the snapshot** — otherwise small charges continue.

---

## 10. Gotchas Checklist (learned the hard way, already fixed in code)

- [x] `SPRING_CLOUD_CONFIG_ENABLED=false` or the app **will not boot** (config server check).
- [x] `throttlegate.default-limits` with `tier:endpoint` keys must be in **YAML**, not `.properties` (colon breaks properties parsing).
- [x] Redis Lua scripts must be valid Lua — no concatenation without separators (fixed with text blocks).
- [x] Redis `RedisTemplate` serializes script args as **strings** — pass `String.valueOf(...)`.
- [x] The custom metrics endpoint must **not** live under `/actuator/metrics` (real Actuator owns that path) — it's at `/api/metrics/throttlegate.requests`.
- [x] CORS must allow the dashboard origin: `THROTTLEGATE_CORS_ALLOWED_ORIGINS`.
- [ ] JVM heap on 1 GB: cap with `-Xmx256m`.
- [ ] Do not expose `:8080` publicly without a reason; prefer nginx reverse proxy.

---

## 11. Open Decisions (resolve at implementation time)

1. **Account type** — new (credit model) vs. old (12-month model) → affects budget thresholds and whether to run 24/7.
2. **Terraform vs Console** — recommended Terraform if you'll redeploy; Console is fine for a one-off demo.
3. **Public API or nginx-only** — is `/v1/check` meant to be called by external clients, or just demoed with the dashboard? Determines the `:8080` security-group rule.
4. **HTTPS/domain** — a real domain + ACM cert via nginx is a follow-up; plain IP over HTTP is fine for a demo.
5. **Dashboard hosting** — same-instance nginx (recommended) vs S3 static + CloudFront.

---

*Plan status: ready for implementation. Phases 0–7 take ~2–3 hours total, most of it waiting on EC2/install steps.*

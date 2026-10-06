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
- [x] **Done already** — `/actuator/health` and `/api/metrics/**` are public (GET) so nginx/CloudWatch/compose probes get 200 instead of 401; actuator runs on the **same port 8080** (no second port to open in the security group).
- [x] **Done already** — Redis-outage resilience: decisions keep being served from an in-process limiter (`throttlegate.resilience.mode`, default `fail-open`); degraded state shows in `/actuator/health` and the `throttlegate.requests.fallback` metric.
- [x] **Done already** — all rate-limit Redis keys carry TTLs (idle clients no longer leak memory on a 1 GB instance).
- [ ] **To do (build time):** make the dashboard API URL build-configurable so the build points at the EC2 host — the dashboard is **Vite**, so use `VITE_API_URL` and the output goes to `dist/`:
  ```bash
  cd throttle-gate-dashboard
  VITE_API_URL=http://<EC2-PUBLIC-IP> npm run build   # output: dist/
  ```
- [x] **Done already (deploy/)** — ready-made artifacts in `deploy/`: `user-data.sh` (Phase 3), `throttlegate.service` (Phase 4), `nginx-throttlegate.conf` (Phase 6 dashboard hosting). The systemd unit sets `-Xms128m -Xmx512m` for 1 GB RAM.

### 3.2 Env vars the app needs on EC2

| Env var | Value | Notes |
|---|---|---|
| `SPRING_CLOUD_CONFIG_ENABLED` | `false` | **Critical — app won't boot without it** |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/throttlegate` | |
| `SPRING_DATASOURCE_USERNAME` | `throttlegate` | |
| `SPRING_DATASOURCE_PASSWORD` | `<pick-one>` | |
| `SPRING_REDIS_HOST` | `localhost` | Valkey on the same instance |
| `SPRING_REDIS_PORT` | `6379` | |
| `THROTTLEGATE_CORS_ALLOWED_ORIGINS` | dashboard origin(s) | e.g. `http://<EC2-IP>` or `https://your-domain` |
| `THROTTLEGATE_RESILIENCE_MODE` | `fail-open` | allow (default) or deny traffic while Redis is down |
| `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD` | admin creds | protects `/actuator/prometheus` etc. |
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
3. Create `/etc/systemd/system/throttlegate.service` — a ready-made unit is shipped at **`deploy/throttlegate.service`** (already sets heap sizing for 1 GB RAM and `THROTTLEGATE_RESILIENCE_MODE=fail-open`); copy it and set the DB password:
   ```bash
   scp -i throttlegate-key.pem deploy/throttlegate.service ec2-user@<EC2-IP>:/tmp/
   ssh -i throttlegate-key.pem ec2-user@<EC2-IP> \
     "sudo sed -i s/change-me/<your-db-password>/ /tmp/throttlegate.service && sudo mv /tmp/throttlegate.service /etc/systemd/system/"
4. Enable + start:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now throttlegate
   sudo journalctl -u throttlegate -f   # watch the boot log
   ```

---

## 7. Phase 5 — Smoke Tests (what "it works" means)

```bash
# App is alive (public — no credentials needed)
curl -s http://localhost:8080/actuator/health            # {"status":"UP"}

# Health must stay UP while Redis is briefly stopped (fail-open fallback)
sudo systemctl stop valkey && curl -s http://localhost:8080/actuator/health && sudo systemctl start valkey
# → {"status":"OUT_OF_SERVICE", ..."rateLimiter":"fallback (in-memory)"} while degraded

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

**Dashboard deployment (same instance, simplest path):** the dashboard is a **Vite** app — the build output is `dist/` and the API URL env var is `VITE_API_URL` (not `REACT_APP_API_URL`/`build/`):
```bash
cd throttle-gate-dashboard
VITE_API_URL=http://<EC2-IP> npm run build
scp -r -i throttlegate-key.pem dist/* ec2-user@<EC2-IP>:/usr/share/nginx/html/
```
Then install the reverse proxy config so the dashboard can call the API on the same origin (no CORS issues):
```bash
scp -i throttlegate-key.pem deploy/nginx-throttlegate.conf ec2-user@<EC2-IP>:/tmp/
ssh -i throttlegate-key.pem ec2-user@<EC2-IP> "sudo install /tmp/nginx-throttlegate.conf /etc/nginx/conf.d/throttlegate.conf && sudo nginx -t && sudo systemctl reload nginx"
```
With nginx proxying `/v1/`, `/api/` and `/actuator/health` to the app, build the dashboard with `VITE_API_URL=` **empty** (same-origin) and it just works.

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
- [x] `/actuator/health` must be **public** or every probe (nginx, CloudWatch, compose healthcheck) gets a 401 — now permitted for GET in `SecurityConfig`.
- [x] Actuator stays on **8080** (was 8081) — one port to open, probes work as documented.
- [x] Rate-limit Redis keys have TTLs — without them a 1 GB t3.micro slowly fills up.
- [x] JVM heap on 1 GB: capped at `-Xmx512m` in `deploy/throttlegate.service`.
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

#!/bin/bash
# EC2 user-data bootstrap for Amazon Linux 2023 (t3.micro)
set -euxo pipefail

# --- Java 17 (matches the project's target; JDK 24 is not used) ---
dnf install -y java-17-amazon-corretto-devel

# --- PostgreSQL 15 ---
dnf install -y postgresql15-server
postgresql-setup --initdb
systemctl enable --now postgresql
sudo -u postgres psql -c "CREATE USER throttlegate WITH PASSWORD 'change-me';"
sudo -u postgres psql -c "CREATE DATABASE throttlegate OWNER throttlegate;"

# --- Valkey (Redis-compatible, packaged on AL2023) ---
dnf install -y valkey
sed -i 's/^bind .*/bind 127.0.0.1/' /etc/valkey/valkey.conf
sed -i 's/^protected-mode .*/protected-mode yes/' /etc/valkey/valkey.conf
systemctl enable --now valkey

# --- nginx (serves the dashboard build + reverse-proxies the API) ---
dnf install -y nginx
install -m 644 /opt/throttlegate/deploy/nginx-throttlegate.conf /etc/nginx/conf.d/throttlegate.conf
systemctl enable nginx

# --- Service user + app dir ---
useradd -r -m -d /opt/throttlegate throttlegate || true
mkdir -p /opt/throttlegate/app /opt/throttlegate/deploy
install -m 644 /opt/throttlegate/deploy/throttlegate.service /etc/systemd/system/throttlegate.service
systemctl daemon-reload

echo "Bootstrap done. Copy the built jar to /opt/throttlegate/app/throttle-gate.jar and: systemctl enable --now throttlegate"

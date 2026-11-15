# Chafgram — development tasks
# https://just.systems — run `just` or `just --list` to see everything.

set shell := ["bash", "-euo", "pipefail", "-c"]

backend_dir := "backend"
frontend_dir := "frontend"

mod backend "backend/justfile"
mod frontend "frontend/justfile"

# List available recipes
default:
    @just --list --list-submodules

# Install backend and frontend dependencies
install: backend::install frontend::install

# Check local toolchain (JDK 21, Node, npm)
doctor:
    #!/usr/bin/env bash
    set -euo pipefail
    if java -version >/dev/null 2>&1; then
      raw=$(java -version 2>&1 | head -n1)
      major=$(printf '%s' "$raw" | grep -oE '(1\.)?[0-9]+' | head -n1 | sed -E 's/^1\.//')
      if [ "$major" = "21" ]; then
        echo "java: OK ($raw)"
      else
        echo "java: WARNING — $raw, but backend targets Java 21 (pom.xml)"
      fi
    else
      echo "java: MISSING (need JDK 21)"
    fi
    command -v node >/dev/null && echo "node: $(node -v)" || echo "node: MISSING"
    command -v npm >/dev/null && echo "npm: $(npm -v)" || echo "npm: MISSING"
    echo "backend needs local MySQL for dev and tests — run it with: just mysql (reset with: just seed)"
    echo "  (compose.yaml dev defaults; override via git-ignored .env)"

# Start local MySQL 8.0 and wait until it reports healthy
mysql:
    docker compose up -d --wait mysql

# Apply Flyway migrations to the dev database (no data is touched)
migrate: mysql
    cd {{backend_dir}} && ./mvnw -q flyway:migrate

# Reset the dev database to full fixtures (WIPES ALL dev data)
# Every table seeded except user_entities/user_credentials (passkeys — register via UI) and flyway_schema_history.
# admin / members with password `chafgram-dev-password`; reusable demo invite token: demo-invite-token
seed: mysql
    #!/usr/bin/env bash
    set -euo pipefail
    read -r -p "This DELETES ALL DATA in the dev chafgram database. Type 'yes' to continue: " confirm
    if [ "$confirm" != "yes" ]; then echo "aborted"; exit 1; fi
    docker compose exec -T mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS chafgram; CREATE DATABASE chafgram CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    cd {{backend_dir}} && ./mvnw -q flyway:migrate
    docker compose exec -T mysql mysql -uroot -proot chafgram < "{{justfile_directory()}}/scripts/dev-seed.sql"
    echo "dev database reset and seeded (admin@chafgram.test / password: chafgram-dev-password)"

# Empty the dev database and re-apply migrations (WIPES ALL dev data, no fixtures)
unseed: mysql
    #!/usr/bin/env bash
    set -euo pipefail
    read -r -p "This DELETES ALL DATA in the dev chafgram database (leaves empty, migrated schema). Type 'yes' to continue: " confirm
    if [ "$confirm" != "yes" ]; then echo "aborted"; exit 1; fi
    docker compose exec -T mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS chafgram; CREATE DATABASE chafgram CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    cd {{backend_dir}} && ./mvnw -q flyway:migrate
    echo "dev database emptied and migrated"

# Run backend and frontend together (Ctrl-C stops both)
# Requires MySQL — run `just mysql` first.
[parallel]
dev: backend::dev frontend::dev

# Build backend jar (target/) and frontend production bundle
build: backend::build frontend::build

# Run backend and frontend tests (backend wipes the dev chafgram schema; restore afterwards with: just seed)
test: backend::test frontend::test

# Verify the repo is healthy: run just test and just build
# (run before pushing or on a fresh clone)
verify: test build

# Remove build artifacts in backend and frontend (not node_modules, not .agents, not sources)
clean: backend::clean frontend::clean

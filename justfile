# Chafgram — development tasks
# https://just.systems — run `just` or `just --list` to see everything.

set shell := ["bash", "-euo", "pipefail", "-c"]

backend_dir := "backend"
frontend_dir := "frontend"

# List available recipes
default:
    @just --list

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
    echo "backend needs local MySQL — run it with: just mysql"
    echo "  (compose.yaml dev defaults; override via git-ignored .env)"

# Install frontend dependencies from the lockfile
setup:
    cd {{frontend_dir}} && npm ci

# Start local MySQL 8.0 and wait until it reports healthy
mysql:
    docker compose up -d --wait mysql

# Backwards-compatible alias for the old db-up recipe
alias db-up := mysql

# Stop local dev services (data volume is kept)
db-down:
    docker compose down

# Tail MySQL logs
db-logs:
    docker compose logs -f mysql

# Run backend and frontend together (Ctrl-C stops both)
# Requires MySQL — run `just mysql` first.
dev:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "backend → http://localhost:8080"
    echo "frontend → http://localhost:3000"
    trap 'kill $(jobs -p) 2>/dev/null || true' EXIT INT TERM
    (cd {{backend_dir}} && ./mvnw spring-boot:run) &
    (cd {{frontend_dir}} && npm run dev) &
    wait

# Run only the backend (port 8080; requires just mysql first)
dev-backend:
    cd {{backend_dir}} && ./mvnw spring-boot:run

# Run only the frontend (port 3000)
dev-frontend:
    cd {{frontend_dir}} && npm run dev

# Build backend jar (target/) and frontend production bundle
build: build-backend build-frontend

build-backend:
    cd {{backend_dir}} && ./mvnw package

build-frontend:
    cd {{frontend_dir}} && npm run build

# Run backend tests (frontend has no test script yet)
test:
    cd {{backend_dir}} && ./mvnw test

# Pre-commit sanity check: backend tests + frontend production build
check: test build-frontend

# Serve the frontend production build
preview:
    cd {{frontend_dir}} && npm run preview

# Remove build artifacts (not node_modules, not .agents, not sources)
clean:
    rm -rf {{backend_dir}}/target {{frontend_dir}}/.output {{frontend_dir}}/dist

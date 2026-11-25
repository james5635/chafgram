# Chafgram — development tasks
# https://just.systems — run `just` or `just --list` to see everything.

set shell := ["bash", "-euo", "pipefail", "-c"]
set dotenv-load := true

backend_dir := "backend"
frontend_dir := "frontend"

mod backend "backend/justfile"
mod frontend "frontend/justfile"

# List available recipes
default:
    @just --list --list-submodules

# Generate GraphQL types/documents from the backend schema (frontend codegen)
generate: frontend::generate

# Install backend and frontend dependencies
install: backend::install frontend::install

# Install local git hooks (run once after cloning)
# The hook source is tracked in scripts/hooks/; this copies it into .git/hooks/.
bootstrap:
    #!/usr/bin/env bash
    set -euo pipefail
    hooks_dir="$(git rev-parse --git-path hooks)"
    src="{{justfile_directory()}}/scripts/hooks/pre-push"
    dest="$hooks_dir/pre-push"
    mkdir -p "$hooks_dir"
    if [ -e "$dest" ]; then
      if cmp -s "$src" "$dest"; then
        echo "pre-push: already installed"
      else
        echo "pre-push: $dest exists and differs from the repo hook; not overwriting" >&2
        echo "         resolve it manually, then re-run 'just bootstrap'" >&2
        exit 1
      fi
    else
      install -m 755 "$src" "$dest"
      echo "pre-push: installed -> $dest"
    fi
    echo "bootstrap complete — 'just verify' runs before every push"

# Check local toolchain (JDK 21, Node, pnpm, Docker, ffmpeg)
doctor:
    #!/usr/bin/env bash
    set -euo pipefail
    if java -version >/dev/null 2>&1; then
      raw=$(java -version 2>&1 | head -n1)
      major=$(printf '%s' "$raw" | grep -oE '(1\.)?[0-9]+' | head -n1 | sed -E 's/^1\.//')
      if [ "$major" = "21" ]; then
        echo "java: OK ($raw)"
      else
        echo "java: WARNING — $raw, but backend targets Java 21 (build.gradle.kts)"
      fi
    else
      echo "java: MISSING (need JDK 21)"
    fi
    command -v node >/dev/null && echo "node: $(node -v)" || echo "node: MISSING"
    command -v pnpm >/dev/null && echo "pnpm: $(pnpm -v)" || echo "pnpm: MISSING"
    command -v docker >/dev/null && echo "docker: $(docker --version)" || echo "docker: MISSING (needed by compose + Testcontainers)"
    command -v ffmpeg >/dev/null && echo "ffmpeg: $(ffmpeg -version 2>/dev/null | head -n1)" || echo "ffmpeg: MISSING (video processing only; images work without it)"
    echo "backend needs local infrastructure for dev — run it with: just infra-up (reset with: just seed)"
    echo "  (backend tests are hermetic — Testcontainers starts its own MySQL 8)"
    echo "  (compose.yaml dev defaults; override via git-ignored .env)"

# Start local infrastructure (MySQL 8.0 + Garage object storage) and wait until healthy
infra-up:
    docker compose up -d --wait mysql garage garage-init

# Stop local infrastructure
infra-down:
    docker compose down

# Apply Flyway migrations to the dev database (no data is touched)
migrate: infra-up
    cd "{{justfile_directory()}}/{{backend_dir}}" && ./gradlew -q :api:flywayMigrate

# Reset the dev database to full fixtures (WIPES ALL dev data)
# Every table seeded except user_entities/user_credentials (passkeys — register via UI) and flyway_schema_history.
# admin / members with password `chafgram-dev-password`; reusable demo invite token: demo-invite-token
seed: infra-up
    #!/usr/bin/env bash
    set -euo pipefail
    read -r -p "This DELETES ALL DATA in the dev chafgram database. Type 'yes' to continue: " confirm
    if [ "$confirm" != "yes" ]; then echo "aborted"; exit 1; fi
    docker compose exec -T mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS chafgram; CREATE DATABASE chafgram CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    cd "{{justfile_directory()}}/{{backend_dir}}" && ./gradlew -q :api:flywayMigrate
    cd "{{justfile_directory()}}/{{backend_dir}}" && ./gradlew -q :seed:bootRun
    echo "dev database reset and seeded (admin@chafgram.test / password: chafgram-dev-password)"

# Empty the dev database and re-apply migrations (WIPES ALL dev data, no fixtures)
unseed: infra-up
    #!/usr/bin/env bash
    set -euo pipefail
    read -r -p "This DELETES ALL DATA in the dev chafgram database (leaves empty, migrated schema). Type 'yes' to continue: " confirm
    if [ "$confirm" != "yes" ]; then echo "aborted"; exit 1; fi
    docker compose exec -T mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS chafgram; CREATE DATABASE chafgram CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    cd "{{justfile_directory()}}/{{backend_dir}}" && ./gradlew -q :api:flywayMigrate
    echo "dev database emptied and migrated"

# Run backend and frontend together (Ctrl-C stops both)
# Requires infrastructure — run `just infra-up` first.
[parallel]
dev: backend::dev frontend::dev

# Build backend jar (backend/api/build/libs/) and frontend production bundle
build: backend::build frontend::build

# Run backend and frontend tests (backend tests are hermetic — Testcontainers MySQL)
test: backend::test frontend::test

# Verify the repo is healthy: run just test and just build
# (run before pushing or on a fresh clone)
verify: test build

# Remove build artifacts in backend and frontend (not node_modules, not .agents, not sources)
clean: backend::clean frontend::clean

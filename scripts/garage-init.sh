#!/bin/sh
# One-shot dev init for the local Garage S3 bucket `chafgram-media`.
# Dev-only: this initializes the throwaway dev instance in compose.yaml
# (scripts/garage.toml). Safe to re-run. The pinned Garage dev build serves
# the admin API v1 surface.
set -eu

AUTH="Authorization: Bearer ${GARAGE_ADMIN_TOKEN:-c550efd14088f04414caa03396fc2785baa912ee9f3bbef18dea0612d8cac0f6}"
BASE=http://garage:3903
CTYPE='content-type: application/json'
ACCESS_KEY="${GARAGE_ACCESS_KEY:-GK67304d5c49c3278bab342460}"
SECRET_KEY="${GARAGE_SECRET_KEY:-d53617c73b0711ab876d08d4072f4f45a65b7f0a150fcf00d38133737d0040f3}"

echo "waiting for garage health…"
for i in $(seq 1 60); do
  if curl -sf -H "$AUTH" "$BASE/v1/health" >/dev/null 2>&1; then break; fi
  sleep 2
done

STATUS=$(curl -sf -H "$AUTH" "$BASE/v1/status")
NODE=$(echo "$STATUS" | jq -r '.nodes[0].id')
ROLE=$(echo "$STATUS" | jq -c '.nodes[0].role')
LAYOUT_VERSION=$(echo "$STATUS" | jq -r '.layoutVersion')
echo "node=$NODE role=$ROLE layout=$LAYOUT_VERSION"

# 1. layout — single node, zone dc1, 1 GB capacity. Skip if already assigned.
if [ "$ROLE" = "null" ] || [ -z "$ROLE" ]; then
  echo "assigning layout…"
  curl -sf -X POST -H "$AUTH" -H "$CTYPE" \
    -d "[{\"id\":\"$NODE\",\"zone\":\"dc1\",\"capacity\":1000000000,\"tags\":[]}]" \
    "$BASE/v1/layout" >/dev/null
  curl -sf -X POST -H "$AUTH" -H "$CTYPE" \
    -d '{"version":1}' \
    "$BASE/v1/layout/apply" >/dev/null
else
  echo "layout already assigned"
fi

# 2. dev access key — import is fine to fail if it already exists.
if ! curl -sf -X POST -H "$AUTH" -H "$CTYPE" \
    -d "{\"accessKeyId\":\"$ACCESS_KEY\",\"name\":\"chafgram\",\"secretAccessKey\":\"$SECRET_KEY\"}" \
    "$BASE/v1/key/import" >/dev/null 2>&1; then
  echo "key already exists"
fi

# 3. bucket — create if missing, otherwise reuse the existing id.
BUCKET_ID=$(curl -sf -H "$AUTH" "$BASE/v1/bucket" \
  | jq -r '.[] | select(.globalAliases[]? == "chafgram-media") | .id')
if [ -z "$BUCKET_ID" ] || [ "$BUCKET_ID" = "null" ]; then
  echo "creating bucket chafgram-media…"
  BUCKET_ID=$(curl -sf -X POST -H "$AUTH" -H "$CTYPE" \
    -d '{"globalAlias":"chafgram-media"}' \
    "$BASE/v1/bucket" | jq -r '.id')
else
  echo "bucket exists (id=$BUCKET_ID)"
fi

# 4. permissions — allow read/write for the dev key (idempotent).
curl -sf -X POST -H "$AUTH" -H "$CTYPE" \
  -d "{\"bucketId\":\"$BUCKET_ID\",\"accessKeyId\":\"$ACCESS_KEY\",\"permissions\":{\"read\":true,\"write\":true}}" \
  "$BASE/v1/bucket/allow" >/dev/null

echo "garage ready: bucket=$BUCKET_ID key=$ACCESS_KEY"

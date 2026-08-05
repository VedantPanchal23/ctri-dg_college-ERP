#!/usr/bin/env bash
# Run on the deployment host (invoked by GitHub Actions CD).
# Expects IMAGE_PREFIX, IMAGE_TAG, GHCR_USER, GHCR_TOKEN in the environment.
# Expects .env.prod in the same directory.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

: "${IMAGE_PREFIX:?IMAGE_PREFIX required}"
: "${IMAGE_TAG:=latest}"
: "${GHCR_USER:?GHCR_USER required}"
: "${GHCR_TOKEN:?GHCR_TOKEN required}"

if [[ ! -f .env.prod ]]; then
  echo "ERROR: $ROOT/.env.prod missing. Copy from .env.prod.example and fill secrets."
  exit 1
fi

IMAGE_PREFIX="$(echo "$IMAGE_PREFIX" | tr '[:upper:]' '[:lower:]')"
export IMAGE_PREFIX IMAGE_TAG

COMPOSE=(docker compose
  -f docker-compose.yml
  -f docker-compose.prod.yml
  -f docker-compose.release.yml
  --env-file .env.prod)

echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin

echo "Pulling $IMAGE_PREFIX/app:$IMAGE_TAG and frontend:$IMAGE_TAG"
"${COMPOSE[@]}" pull app frontend

echo "Starting stack (no local build)"
"${COMPOSE[@]}" up -d --no-build

echo "Pruning dangling images"
docker image prune -f >/dev/null || true

echo "Deploy complete"
"${COMPOSE[@]}" ps

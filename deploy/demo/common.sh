#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="${ROOT_DIR:-/opt/secondhand/secondhand-platform-backend}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-.env.prod}"

cd "$ROOT_DIR"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "ERROR: $COMPOSE_FILE not found in $ROOT_DIR" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: $ENV_FILE not found in $ROOT_DIR" >&2
  exit 1
fi

DC=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

section() {
  printf '\n\033[1;36m==== %s ====\033[0m\n' "$*"
}

ok() {
  printf '\033[1;32m[OK]\033[0m %s\n' "$*"
}

warn() {
  printf '\033[1;33m[WARN]\033[0m %s\n' "$*"
}

fail() {
  printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    fail "Missing command: $1"
    exit 1
  }
}

container_id() {
  "${DC[@]}" ps -q "$1"
}

container_name() {
  local id
  id="$(container_id "$1")"
  if [ -z "$id" ]; then
    return 1
  fi
  docker inspect -f '{{.Name}}' "$id" | sed 's#^/##'
}

exec_service() {
  local service="$1"
  shift
  "${DC[@]}" exec -T "$service" "$@"
}

postgres_exec() {
  local db="$1"
  local sql="$2"
  exec_service postgres psql -U "${POSTGRES_USER:-secondhand}" -d "$db" -v ON_ERROR_STOP=1 -c "$sql"
}

load_env_without_printing_secrets() {
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
}

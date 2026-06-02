#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd docker

section "Validate Kong declarative config"
docker run --rm \
  -e KONG_DATABASE=off \
  -v "$ROOT_DIR/gateway/kong:/work" \
  kong:3.8 \
  kong config parse /work/kong.yml

section "Kong configured upstreams / retries / rate limits"
awk '
  /^upstreams:/ {show=1}
  /^services:/ {show=1}
  show && /(name: .*upstream|target:|algorithm:|name: .*service|retries:|connect_timeout:|read_timeout:|write_timeout:|rate-limiting|minute:|hour:)/ {print}
' gateway/kong/kong.yml

section "Docker DNS from Kong container"
for host in auth-service core-service order-service chat-service ai-service redis postgres rabbitmq mongodb; do
  printf '%-16s ' "$host"
  exec_service kong sh -c "getent hosts $host || nslookup $host || true"
done

section "Gateway response headers show rate-limit plugin"
PUBLIC_API_BASE_URL="${PUBLIC_API_BASE_URL:-http://127.0.0.1:8000}"
curl -ksSI "$PUBLIC_API_BASE_URL/core/actuator/health" | sed -n '1,20p'

section "Route smoke tests through Kong"
for path in "/core/actuator/health" "/core/ws-notification/info" "/chat/ws-chat/info"; do
  printf '%-32s ' "$path"
  curl -ksS -o /tmp/kong_route_body -w 'HTTP %{http_code} time=%{time_total}s\n' "$PUBLIC_API_BASE_URL$path" || true
  head -c 120 /tmp/kong_route_body || true
  echo
done

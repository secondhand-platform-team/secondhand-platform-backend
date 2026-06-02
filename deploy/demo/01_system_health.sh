#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd docker
require_cmd curl

section "Docker Compose status"
"${DC[@]}" ps

section "Container health summary"
services=(kong postgres redis mongodb rabbitmq auth-service core-service order-service chat-service ai-service)
for service in "${services[@]}"; do
  id="$(container_id "$service" || true)"
  if [ -z "$id" ]; then
    warn "$service: container not found"
    continue
  fi
  name="$(docker inspect -f '{{.Name}}' "$id" | sed 's#^/##')"
  state="$(docker inspect -f '{{.State.Status}}' "$id")"
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$id")"
  restarts="$(docker inspect -f '{{.RestartCount}}' "$id")"
  printf '%-18s %-45s state=%-10s health=%-15s restarts=%s\n' "$service" "$name" "$state" "$health" "$restarts"
done

section "Local gateway health probes"
probe() {
  local label="$1"
  local url="$2"
  printf '%-28s ' "$label"
  curl -ksS -o /tmp/demo_probe_body -w 'HTTP %{http_code} time=%{time_total}s\n' "$url" || true
  head -c 180 /tmp/demo_probe_body || true
  echo
}

probe "Kong root" "http://127.0.0.1:8000/"
probe "Core actuator" "http://127.0.0.1:8000/core/actuator/health"
probe "Core SockJS info" "http://127.0.0.1:8000/core/ws-notification/info"
probe "Chat SockJS info" "http://127.0.0.1:8000/chat/ws-chat/info"

section "Public API health probes"
PUBLIC_API_BASE_URL="${PUBLIC_API_BASE_URL:-https://vinalo.fit}"
probe "Public core health" "$PUBLIC_API_BASE_URL/core/actuator/health"
probe "Public notification WS info" "$PUBLIC_API_BASE_URL/core/ws-notification/info"
probe "Public chat WS info" "$PUBLIC_API_BASE_URL/chat/ws-chat/info"

section "Resource snapshot"
docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}' || true

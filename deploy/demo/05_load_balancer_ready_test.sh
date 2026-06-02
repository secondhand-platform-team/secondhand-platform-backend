#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd docker
require_cmd curl

section "Load balancer readiness - config proof"
echo "Kong uses upstreams with round-robin algorithm and target lists. Current targets:"
awk '
  /^  - name: .*upstream/ {up=$0; print "\n" up}
  /algorithm:/ {print}
  /target:/ {print}
' gateway/kong/kong.yml

section "Current service replica count"
for service in auth-service core-service order-service chat-service ai-service; do
  count="$("${DC[@]}" ps -q "$service" | wc -l | tr -d ' ')"
  printf '%-16s replicas=%s\n' "$service" "$count"
done

section "Kong can resolve upstream service names"
for service in auth-service core-service order-service chat-service ai-service; do
  printf '%-16s ' "$service"
  exec_service kong sh -c "getent hosts $service || true"
done

if [ "${LB_SCALE_DEMO:-false}" != "true" ]; then
  warn "Safe mode: not scaling production containers."
  echo "To demo actual scale-out on EC2, run: LB_SCALE_DEMO=true SCALE_SERVICE=core-service SCALE_TO=2 $0"
  exit 0
fi

SCALE_SERVICE="${SCALE_SERVICE:-core-service}"
SCALE_TO="${SCALE_TO:-2}"

case "$SCALE_SERVICE" in
  auth-service|core-service|order-service|chat-service|ai-service) ;;
  *) fail "Unsupported SCALE_SERVICE=$SCALE_SERVICE"; exit 1 ;;
esac

section "Scaling $SCALE_SERVICE to $SCALE_TO replicas"
warn "This changes running containers. Use only during demo window."
"${DC[@]}" up -d --no-recreate --scale "$SCALE_SERVICE=$SCALE_TO" "$SCALE_SERVICE"
"${DC[@]}" ps "$SCALE_SERVICE"

section "Warm-up and route test"
for i in $(seq 1 10); do
  curl -ksS -o /tmp/lb_body -w "request=$i HTTP %{http_code} time=%{time_total}s\n" "http://127.0.0.1:8000/core/actuator/health" || true
done

section "Reminder to scale back if needed"
echo "Scale back command:"
echo "cd $ROOT_DIR && docker compose --env-file $ENV_FILE -f $COMPOSE_FILE up -d --scale $SCALE_SERVICE=1 $SCALE_SERVICE && docker compose --env-file $ENV_FILE -f $COMPOSE_FILE restart kong"

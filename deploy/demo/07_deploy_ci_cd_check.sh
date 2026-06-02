#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd docker
require_cmd curl

section "CI/CD deployment state"
echo "Current git commit on EC2:"
git log --oneline -n 5

echo
section "Git remotes"
git remote -v

echo
section "Compose config validation"
"${DC[@]}" config >/tmp/demo_compose_config.yml
ok "docker compose config rendered successfully"

section "Last deployment-relevant container start times"
for service in auth-service core-service order-service chat-service ai-service kong; do
  id="$(container_id "$service" || true)"
  if [ -z "$id" ]; then
    warn "$service not found"
    continue
  fi
  printf '%-16s %s\n' "$service" "$(docker inspect -f '{{.State.StartedAt}}' "$id")"
done

section "Recent service startup/Flyway logs"
for service in auth-service core-service order-service chat-service ai-service; do
  echo "--- $service ---"
  "${DC[@]}" logs --tail=120 "$service" | grep -Ei 'Started .*Application|Flyway|Migration|Tomcat started|ERROR|WARN' || true
  echo
done

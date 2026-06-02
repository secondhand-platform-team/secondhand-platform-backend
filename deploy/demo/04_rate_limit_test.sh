#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd curl

PUBLIC_API_BASE_URL="${PUBLIC_API_BASE_URL:-http://127.0.0.1:8000}"
TARGET_PATH="${TARGET_PATH:-/auth/api/auth/me}"
REQUESTS="${REQUESTS:-70}"

section "Rate limit demo"
echo "Target: $PUBLIC_API_BASE_URL$TARGET_PATH"
echo "Requests: $REQUESTS"
echo

if [ "${RATE_LIMIT_STRESS:-false}" != "true" ]; then
  warn "Safe mode: only sending 5 requests. Set RATE_LIMIT_STRESS=true to intentionally hit limit."
  REQUESTS=5
fi

for i in $(seq 1 "$REQUESTS"); do
  code="$(curl -ksS -o /tmp/rate_body -w '%{http_code}' "$PUBLIC_API_BASE_URL$TARGET_PATH" || true)"
  printf '%03d -> HTTP %s' "$i" "$code"
  if [ "$code" = "429" ]; then
    printf '  <-- rate limited'
  fi
  printf '\n'
  if [ "$code" = "429" ]; then
    echo
    section "429 response body"
    cat /tmp/rate_body || true
    echo
    exit 0
  fi
done

echo
if [ "${RATE_LIMIT_STRESS:-false}" = "true" ]; then
  warn "Did not receive 429. Check route, policy, Redis availability, and whether another identifier is used for rate limit."
else
  ok "Safe smoke test completed. For demo 429: RATE_LIMIT_STRESS=true REQUESTS=70 $0"
fi

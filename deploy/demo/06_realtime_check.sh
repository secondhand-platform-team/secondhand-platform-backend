#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd curl

PUBLIC_API_BASE_URL="${PUBLIC_API_BASE_URL:-https://vinalo.fit}"

section "SockJS/WebSocket endpoint checks"
for url in \
  "$PUBLIC_API_BASE_URL/core/ws-notification/info" \
  "$PUBLIC_API_BASE_URL/chat/ws-chat/info" \
  "http://127.0.0.1:8000/core/ws-notification/info" \
  "http://127.0.0.1:8000/chat/ws-chat/info"; do
  echo "URL: $url"
  curl -ksS -i "$url" | sed -n '1,16p'
  echo
done

section "Recent websocket logs"
"${DC[@]}" logs --tail=80 core-service | grep -Ei 'websocket|ws-notification|notification|sockjs|handshake' || true
"${DC[@]}" logs --tail=80 chat-service | grep -Ei 'websocket|ws-chat|sockjs|presence|handshake|connected' || true

#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for script in \
  01_system_health.sh \
  02_db_flyway_report.sh \
  03_gateway_policy_test.sh \
  06_realtime_check.sh \
  07_deploy_ci_cd_check.sh; do
  echo
  echo "######## Running $script ########"
  bash "$SCRIPT_DIR/$script"
done

echo
echo "######## Optional scripts not auto-run ########"
echo "Rate limit stress: RATE_LIMIT_STRESS=true bash $SCRIPT_DIR/04_rate_limit_test.sh"
echo "LB scale demo:     LB_SCALE_DEMO=true SCALE_SERVICE=core-service SCALE_TO=2 bash $SCRIPT_DIR/05_load_balancer_ready_test.sh"

#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

load_env_without_printing_secrets
require_cmd docker

section "PostgreSQL database list"
exec_service postgres psql -U "${POSTGRES_USER:-secondhand}" -d postgres -v ON_ERROR_STOP=1 -c "
select datname as database, pg_size_pretty(pg_database_size(datname)) as size
from pg_database
where datistemplate = false
order by datname;
"

report_flyway() {
  local db="$1"
  section "Flyway history: $db"
  postgres_exec "$db" "
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;
"
}

report_flyway secondhand_auth_db
report_flyway secondhand_core_db
report_flyway secondhand_order_db

section "Important order/payment schema checks"
postgres_exec secondhand_order_db "
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema='public' and table_name='payments'
  and column_name in ('transaction_id','vnp_txn_ref','status','amount','order_id')
order by column_name;
"

postgres_exec secondhand_order_db "
select conname, pg_get_constraintdef(c.oid) as definition
from pg_constraint c
join pg_class t on t.oid = c.conrelid
where t.relname='orders' and conname like '%status%'
order by conname;
"

section "Business state summary"
postgres_exec secondhand_order_db "
select status, payment_status, count(*)
from orders
group by status, payment_status
order by status, payment_status;
"

postgres_exec secondhand_order_db "
select status, count(*)
from payments
group by status
order by status;
"

postgres_exec secondhand_core_db "
select status, count(*)
from items
group by status
order by status;
"

section "MongoDB chat database summary"
exec_service mongodb mongosh secondhand_chat_db --quiet --eval '
printjson({collections: db.getCollectionNames()});
for (const name of db.getCollectionNames()) {
  print(name + ": " + db.getCollection(name).countDocuments());
}
' || warn "MongoDB summary failed"

section "Redis basic summary"
exec_service redis redis-cli INFO keyspace || true

# SQL Demo Queries

Các file SQL này chỉ chứa query read-only để demo DB/Flyway/business state.

## Chạy order DB

```bash
cd /opt/secondhand/secondhand-platform-backend
docker exec -i secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db \
  < deploy/demo/sql/order_demo_queries.sql
```

## Chạy core DB

```bash
cd /opt/secondhand/secondhand-platform-backend
docker exec -i secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db \
  < deploy/demo/sql/core_demo_queries.sql
```

## Chạy auth DB

```bash
cd /opt/secondhand/secondhand-platform-backend
docker exec -i secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_auth_db \
  < deploy/demo/sql/auth_demo_queries.sql
```

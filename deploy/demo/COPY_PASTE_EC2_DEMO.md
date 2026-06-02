# EC2 Copy-Paste Demo Commands

> File này dùng cho lúc báo cáo/demo: chỉ cần copy từng block vào terminal EC2. Tất cả lệnh mặc định là read-only/an toàn, trừ các block có ghi rõ `OPTIONAL STRESS` hoặc `OPTIONAL SCALE`.

## 0. Vào đúng thư mục project

```bash
cd /opt/secondhand/secondhand-platform-backend
chmod +x deploy/demo/*.sh
```

Nếu chưa có script trên EC2, pull/deploy qua GitLab CI hoặc chạy tạm:

```bash
git fetch origin
git checkout main
git reset --hard origin/main
chmod +x deploy/demo/*.sh
```

## 1. Demo nhanh toàn hệ thống

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/00_run_safe_demo_checks.sh
```

Block này chạy các phần an toàn:

- Docker/container health
- DB/Flyway report
- Kong gateway policy
- WebSocket/SockJS check
- CI/CD deploy status

## 2. Docker và container health

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/01_system_health.sh
```

Nếu muốn chạy lệnh raw thay vì script:

```bash
cd /opt/secondhand/secondhand-platform-backend

docker compose --env-file .env.prod -f docker-compose.prod.yml ps

docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}'

curl -i http://127.0.0.1:8000/core/actuator/health
curl -i https://vinalo.fit/core/actuator/health
```

## 3. DB/Flyway report tổng hợp

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/02_db_flyway_report.sh
```

## 4. PostgreSQL - danh sách database và size

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d postgres -c "
select datname as database, pg_size_pretty(pg_database_size(datname)) as size
from pg_database
where datistemplate = false
order by datname;
"
```

## 5. Flyway auth DB

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_auth_db -c "
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;
"
```

## 6. Flyway core DB

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db -c "
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;
"
```

## 7. Flyway order DB

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;
"
```

## 8. Order/payment schema - kiểm tra cột VNPay reference

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema='public'
  and table_name='payments'
  and column_name in ('id','transaction_id','vnp_txn_ref','status','amount','order_id','created_at','updated_at')
order by column_name;
"
```

## 9. Order status constraint

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select conname, pg_get_constraintdef(c.oid) as definition
from pg_constraint c
join pg_class t on t.oid = c.conrelid
where t.relname='orders'
  and conname like '%status%'
order by conname;
"
```

## 10. Tổng quan trạng thái order/payment

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select status, payment_status, count(*)
from orders
group by status, payment_status
order by status, payment_status;
"

docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select status, count(*)
from payments
group by status
order by status;
"
```

## 11. Kiểm tra order pending payment gần nhất

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select id, buyer_id, seller_id, status, payment_status, total_price, created_at, updated_at
from orders
where status='PENDING_PAYMENT'
order by created_at desc
limit 10;
"
```

## 12. Kiểm tra payment gần nhất

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select id, order_id, transaction_id, vnp_txn_ref, amount, status, created_at, updated_at
from payments
order by created_at desc
limit 10;
"
```

## 13. Core DB - trạng thái sản phẩm

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db -c "
select status, count(*)
from items
group by status
order by status;
"
```

## 14. Core DB - sản phẩm mới nhất

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db -c "
select item_id, title, price, status, user_id, created_at, updated_at
from items
order by created_at desc
limit 10;
"
```

## 15. Core DB - kiểm tra item đang RESERVED

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db -c "
select item_id, title, status, reserved_by, reserved_until, updated_at
from items
where status='RESERVED'
order by updated_at desc
limit 10;
"
```

## 16. Core DB - category/attribute count

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db -c "
select 'categories' as table_name, count(*) from categories
union all
select 'category_attributes', count(*) from category_attributes
union all
select 'items', count(*) from items
union all
select 'notifications', count(*) from notifications;
"
```

## 17. Auth DB - user count theo role/status

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_auth_db -c "
select role, status, count(*)
from users
group by role, status
order by role, status;
"
```

Nếu bảng không có cột `status` ở version hiện tại, dùng lệnh fallback:

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_auth_db -c "
select role, count(*)
from users
group by role
order by role;
"
```

## 18. Auth DB - user mới nhất, không in password

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_auth_db -c "
select id, email, phone_number, role, created_at
from users
order by created_at desc
limit 10;
"
```

## 19. MongoDB chat summary

```bash
docker exec -it secondhand-platform-backend-mongodb-1 \
  mongosh secondhand_chat_db --quiet --eval '
printjson({collections: db.getCollectionNames()});
for (const name of db.getCollectionNames()) {
  print(name + ": " + db.getCollection(name).countDocuments());
}
'
```

## 20. MongoDB chat messages gần nhất

```bash
docker exec -it secondhand-platform-backend-mongodb-1 \
  mongosh secondhand_chat_db --quiet --eval '
const collections = db.getCollectionNames();
if (collections.includes("messages")) {
  db.messages.find({}, {_id:1, conversationId:1, senderId:1, receiverId:1, content:1, createdAt:1}).sort({createdAt:-1}).limit(5).forEach(printjson);
} else if (collections.includes("chat_messages")) {
  db.chat_messages.find({}, {_id:1, conversationId:1, senderId:1, receiverId:1, content:1, createdAt:1}).sort({createdAt:-1}).limit(5).forEach(printjson);
} else {
  print("No messages collection found");
}
'
```

## 21. Redis health/keyspace

```bash
docker exec -it secondhand-platform-backend-redis-1 redis-cli PING

docker exec -it secondhand-platform-backend-redis-1 redis-cli INFO keyspace
```

## 22. RabbitMQ health

```bash
docker exec -it secondhand-platform-backend-rabbitmq-1 rabbitmq-diagnostics ping

docker exec -it secondhand-platform-backend-rabbitmq-1 rabbitmqctl list_queues name messages consumers
```

## 23. Kong Gateway config validation

```bash
cd /opt/secondhand/secondhand-platform-backend

docker run --rm \
  -e KONG_DATABASE=off \
  -v "$PWD/gateway/kong:/work" \
  kong:3.8 \
  kong config parse /work/kong.yml
```

## 24. Kong routes/upstream/rate-limit bằng script

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/03_gateway_policy_test.sh
```

## 25. Kong route smoke test thủ công

```bash
curl -i http://127.0.0.1:8000/core/actuator/health
curl -i http://127.0.0.1:8000/core/ws-notification/info
curl -i http://127.0.0.1:8000/chat/ws-chat/info

curl -i https://vinalo.fit/core/actuator/health
curl -i https://vinalo.fit/core/ws-notification/info
curl -i https://vinalo.fit/chat/ws-chat/info
```

## 26. Demo rate limiter qua script

Safe mode:

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/04_rate_limit_test.sh
```

OPTIONAL STRESS - cố tình hit 429:

```bash
cd /opt/secondhand/secondhand-platform-backend
RATE_LIMIT_STRESS=true REQUESTS=70 TARGET_PATH=/auth/api/auth/me bash deploy/demo/04_rate_limit_test.sh
```

## 27. Demo load-balancer-ready

Safe mode:

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/05_load_balancer_ready_test.sh
```

OPTIONAL SCALE - chỉ chạy nếu EC2 đủ tài nguyên:

```bash
cd /opt/secondhand/secondhand-platform-backend
LB_SCALE_DEMO=true SCALE_SERVICE=core-service SCALE_TO=2 bash deploy/demo/05_load_balancer_ready_test.sh
```

Scale back:

```bash
cd /opt/secondhand/secondhand-platform-backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --scale core-service=1 core-service
docker compose --env-file .env.prod -f docker-compose.prod.yml restart kong
```

## 28. Demo realtime WebSocket/SockJS

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/06_realtime_check.sh
```

Raw:

```bash
curl -i https://vinalo.fit/core/ws-notification/info
curl -i https://vinalo.fit/chat/ws-chat/info
```

## 29. Demo CI/CD deploy state

```bash
cd /opt/secondhand/secondhand-platform-backend
bash deploy/demo/07_deploy_ci_cd_check.sh
```

Raw:

```bash
cd /opt/secondhand/secondhand-platform-backend
git log --oneline -n 5
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

## 30. Logs khi cần debug nhanh

```bash
cd /opt/secondhand/secondhand-platform-backend

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=80 kong

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=120 auth-service

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=120 core-service

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=120 order-service

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=120 chat-service
```

## 31. Logs payment/VNPay/order cụ thể

```bash
cd /opt/secondhand/secondhand-platform-backend

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=250 order-service \
  | grep -Ei 'vnpay|payment|callback|transaction|PENDING_PAYMENT|PAID|FAILED|cancel|reserve|release' || true
```

## 32. Logs notification/chat realtime cụ thể

```bash
cd /opt/secondhand/secondhand-platform-backend

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=200 core-service \
  | grep -Ei 'notification|websocket|ws-notification|rabbit|consumer' || true

docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=200 chat-service \
  | grep -Ei 'chat|websocket|ws-chat|presence|message' || true
```

## 33. Nginx/public entrypoint check

```bash
curl -I https://vinalo.fit
curl -I https://vinalo.fit/core/actuator/health

sudo nginx -t
sudo systemctl status nginx --no-pager
```

## 34. Security group/DB exposure check trên EC2

```bash
sudo ss -ltnp | grep -E ':22|:80|:443|:5432|:27017|:6379|:5672|:15672|:8000' || true

docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

Điểm nói khi demo:

- DB không public port trực tiếp ra Internet.
- Admin/dev vào DB bằng `SSH -> docker exec -> psql/mongosh`.
- App connect DB qua Docker network nội bộ.

## 35. Backup nhanh trước khi chạy demo nhạy cảm

Nếu cần backup order DB trước khi thử flow payment:

```bash
mkdir -p ~/db-backups

docker exec secondhand-platform-backend-postgres-1 \
  pg_dump -U secondhand secondhand_order_db \
  > ~/db-backups/secondhand_order_db_$(date +%Y%m%d_%H%M%S).sql

ls -lh ~/db-backups | tail
```

## 36. Checklist thứ tự demo khuyến nghị

Copy-paste lần lượt:

```bash
cd /opt/secondhand/secondhand-platform-backend
chmod +x deploy/demo/*.sh
bash deploy/demo/01_system_health.sh
bash deploy/demo/02_db_flyway_report.sh
bash deploy/demo/03_gateway_policy_test.sh
bash deploy/demo/05_load_balancer_ready_test.sh
bash deploy/demo/06_realtime_check.sh
bash deploy/demo/07_deploy_ci_cd_check.sh
```

Nếu giảng viên hỏi rate limiter:

```bash
RATE_LIMIT_STRESS=true REQUESTS=70 TARGET_PATH=/auth/api/auth/me bash deploy/demo/04_rate_limit_test.sh
```

Nếu giảng viên hỏi DB migration:

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
"
```

Nếu giảng viên hỏi payment/VNPay:

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select id, order_id, transaction_id, vnp_txn_ref, amount, status, created_at, updated_at
from payments
order by created_at desc
limit 10;
"
```

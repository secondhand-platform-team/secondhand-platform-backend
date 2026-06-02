# EC2 Demo Scripts - Quick Start

## Copy-paste file

Open this file when presenting:

```bash
cat deploy/demo/COPY_PASTE_EC2_DEMO.md
```

It contains ready-to-copy commands for Docker, DB/Flyway, PostgreSQL queries, MongoDB, Redis, RabbitMQ, Kong, rate limiter, load-balancer-ready, realtime, logs, Nginx and CI/CD.

## SQL query files

Run full read-only SQL query packs:

```bash
cd /opt/secondhand/secondhand-platform-backend

docker exec -i secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db \
  < deploy/demo/sql/order_demo_queries.sql

docker exec -i secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_core_db \
  < deploy/demo/sql/core_demo_queries.sql

docker exec -i secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_auth_db \
  < deploy/demo/sql/auth_demo_queries.sql
```

---
# EC2 Demo Scripts

CÃ¡c script trong thÆ° má»¥c nÃ y dÃ¹ng Ä‘á»ƒ demo váº­n hÃ nh há»‡ thá»‘ng trÃªn EC2: Docker, DB/Flyway, Kong Gateway, rate limiter, load-balancer-ready, realtime WebSocket/SockJS vÃ  CI/CD deploy.

> Máº·c Ä‘á»‹nh cÃ¡c script Æ°u tiÃªn **read-only/safe mode**. Nhá»¯ng script cÃ³ thá»ƒ táº¡o táº£i hoáº·c thay Ä‘á»•i container yÃªu cáº§u báº­t biáº¿n mÃ´i trÆ°á»ng rÃµ rÃ ng.

## 1. Chuáº©n bá»‹ trÃªn EC2

```bash
cd /opt/secondhand/secondhand-platform-backend
chmod +x deploy/demo/*.sh
```

Náº¿u repo náº±m á»Ÿ Ä‘Æ°á»ng dáº«n khÃ¡c:

```bash
ROOT_DIR=/duong/dan/project bash deploy/demo/01_system_health.sh
```

## 2. Cháº¡y bá»™ kiá»ƒm tra an toÃ n trÆ°á»›c demo

```bash
bash deploy/demo/00_run_safe_demo_checks.sh
```

Script nÃ y cháº¡y:

- `01_system_health.sh`
- `02_db_flyway_report.sh`
- `03_gateway_policy_test.sh`
- `06_realtime_check.sh`
- `07_deploy_ci_cd_check.sh`

KhÃ´ng cháº¡y stress rate-limit vÃ  khÃ´ng scale container.

## 3. Script chi tiáº¿t

### `01_system_health.sh`

Má»¥c tiÃªu demo:

- Docker Compose Ä‘ang cháº¡y nhá»¯ng service nÃ o.
- Health/restart count tá»«ng container.
- Gateway local/public cÃ³ pháº£n há»“i khÃ´ng.
- SockJS info endpoint cá»§a notification/chat cÃ³ hoáº¡t Ä‘á»™ng khÃ´ng.
- Snapshot CPU/RAM container.

Lá»‡nh:

```bash
bash deploy/demo/01_system_health.sh
```

Äiá»ƒm giáº£i thÃ­ch vá»›i giáº£ng viÃªn:

- Há»‡ thá»‘ng khÃ´ng expose DB trá»±c tiáº¿p ra Internet.
- CÃ¡c service cháº¡y trong Docker network ná»™i bá»™.
- Kong/Nginx lÃ  entrypoint public.

### `02_db_flyway_report.sh`

Má»¥c tiÃªu demo:

- DB Ä‘ang cÃ³ nhá»¯ng database nÃ o.
- Flyway migration tá»«ng service Ä‘Ã£ cháº¡y tá»›i version nÃ o.
- Order DB cÃ³ cá»™t `vnp_txn_ref` Ä‘á»ƒ map VNPay callback khÃ´ng.
- Constraint order status Ä‘Ã£ Ä‘Ãºng chÆ°a.
- Tá»•ng quan tráº¡ng thÃ¡i order/payment/item.
- MongoDB/Redis basic summary.

Lá»‡nh:

```bash
bash deploy/demo/02_db_flyway_report.sh
```

Äiá»ƒm giáº£i thÃ­ch:

- Flyway chá»‰ cháº¡y migration chÆ°a apply.
- DB má»›i cháº¡y tá»« V1 tá»›i version má»›i nháº¥t.
- DB production Ä‘Ã£ cÃ³ V1-V3 thÃ¬ deploy má»›i chá»‰ cháº¡y V4.
- KhÃ´ng sá»­a migration Ä‘Ã£ cháº¡y production, pháº£i táº¡o version má»›i.

### `03_gateway_policy_test.sh`

Má»¥c tiÃªu demo:

- Validate Kong YAML.
- Chá»©ng minh Kong cÃ³ upstream, retry, timeout, rate-limiting.
- Kiá»ƒm tra Docker DNS tá»« Kong tá»›i service ná»™i bá»™.
- Smoke test route qua Kong.

Lá»‡nh:

```bash
bash deploy/demo/03_gateway_policy_test.sh
```

Äiá»ƒm giáº£i thÃ­ch:

- Kong config dÃ¹ng upstream round-robin, hiá»‡n má»—i service má»™t target nhÆ°ng sáºµn sÃ ng scale thÃªm replicas.
- Retry giÃºp chá»‹u lá»—i táº¡m thá»i.
- Rate limit báº£o vá»‡ server-side, frontend khÃ´ng thá»ƒ bypass.

### `04_rate_limit_test.sh`

Má»¥c tiÃªu demo:

- Kiá»ƒm tra rate limiter cá»§a Kong.
- Safe mode chá»‰ gá»­i 5 request.
- Stress mode gá»­i nhiá»u request Ä‘á»ƒ cá»‘ tÃ¬nh nháº­n HTTP `429`.

Safe mode:

```bash
bash deploy/demo/04_rate_limit_test.sh
```

Stress mode:

```bash
RATE_LIMIT_STRESS=true REQUESTS=70 TARGET_PATH=/auth/api/auth/me bash deploy/demo/04_rate_limit_test.sh
```

LÆ°u Ã½:

- Auth route limit hiá»‡n lÃ  60 request/phÃºt, nÃªn dÃ¹ng `/auth/...` dá»… demo `429` hÆ¡n.
- KhÃ´ng nÃªn stress quÃ¡ nhiá»u lÃºc Ä‘ang demo live.

### `05_load_balancer_ready_test.sh`

Má»¥c tiÃªu demo:

- Chá»©ng minh cáº¥u hÃ¬nh Kong Ä‘Ã£ load-balancer-ready qua upstream/targets/round-robin.
- Safe mode chá»‰ in config vÃ  replica count.
- Optional mode cÃ³ thá»ƒ scale má»™t service lÃªn 2 replicas.

Safe mode:

```bash
bash deploy/demo/05_load_balancer_ready_test.sh
```

Scale demo tÃ¹y chá»n:

```bash
LB_SCALE_DEMO=true SCALE_SERVICE=core-service SCALE_TO=2 bash deploy/demo/05_load_balancer_ready_test.sh
```

Scale back:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --scale core-service=1 core-service
docker compose --env-file .env.prod -f docker-compose.prod.yml restart kong
```

LÆ°u Ã½:

- Chá»‰ cháº¡y scale demo náº¿u EC2 Ä‘á»§ RAM/CPU.
- ÄÃ¢y lÃ  demo load-balancer-ready, chÆ°a pháº£i autoscaling production-grade.

### `06_realtime_check.sh`

Má»¥c tiÃªu demo:

- Kiá»ƒm tra endpoint SockJS cho notification/chat.
- Xem log websocket gáº§n nháº¥t.

Lá»‡nh:

```bash
bash deploy/demo/06_realtime_check.sh
```

Äiá»ƒm giáº£i thÃ­ch:

- Native WebSocket cÃ³ thá»ƒ fail handshake rá»“i SockJS fallback váº«n káº¿t ná»‘i Ä‘Æ°á»£c.
- Chat vÃ  notification tÃ¡ch service, chat lá»—i khÃ´ng nÃªn lÃ m checkout lá»—i.

### `07_deploy_ci_cd_check.sh`

Má»¥c tiÃªu demo:

- Show commit hiá»‡n táº¡i trÃªn EC2.
- Validate compose.
- Show thá»i Ä‘iá»ƒm container start.
- Show log startup/Flyway gáº§n nháº¥t.

Lá»‡nh:

```bash
bash deploy/demo/07_deploy_ci_cd_check.sh
```

Äiá»ƒm giáº£i thÃ­ch:

- Khi GitLab `deploy_ec2` pass, EC2 Ä‘Ã£ tá»± `git reset --hard origin/main`, build vÃ  recreate services.
- KhÃ´ng cáº§n pull thá»§ cÃ´ng ná»¯a náº¿u deploy job pass.

## 4. Script SQL nhanh náº¿u muá»‘n cháº¡y thá»§ cÃ´ng

### Flyway order DB

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
"
```

### Kiá»ƒm tra cá»™t VNPay reference

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select column_name, data_type
from information_schema.columns
where table_name='payments' and column_name='vnp_txn_ref';
"
```

### Tá»•ng quan tráº¡ng thÃ¡i order/payment

```bash
docker exec -it secondhand-platform-backend-postgres-1 \
  psql -U secondhand -d secondhand_order_db -c "
select status, payment_status, count(*)
from orders
group by status, payment_status
order by status, payment_status;
"
```

## 5. Checklist demo nhanh

1. Cháº¡y health:

```bash
bash deploy/demo/01_system_health.sh
```

2. Cháº¡y DB/Flyway:

```bash
bash deploy/demo/02_db_flyway_report.sh
```

3. Cháº¡y gateway:

```bash
bash deploy/demo/03_gateway_policy_test.sh
```

4. Náº¿u cáº§n demo rate limiter:

```bash
RATE_LIMIT_STRESS=true REQUESTS=70 bash deploy/demo/04_rate_limit_test.sh
```

5. Náº¿u cáº§n demo load-balancer-ready:

```bash
bash deploy/demo/05_load_balancer_ready_test.sh
```

6. Náº¿u cáº§n demo realtime:

```bash
bash deploy/demo/06_realtime_check.sh
```

## 6. Cáº£nh bÃ¡o an toÃ n

- KhÃ´ng public port PostgreSQL/MongoDB ra Internet Ä‘á»ƒ demo.
- KhÃ´ng in `.env.prod` vÃ¬ cÃ³ secret.
- KhÃ´ng cháº¡y stress test lá»›n trÃªn EC2 demo.
- KhÃ´ng scale nhiá»u replicas náº¿u EC2 nhá».
- KhÃ´ng sá»­a migration Ä‘Ã£ cháº¡y production.



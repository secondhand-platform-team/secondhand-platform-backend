\echo '=== ORDER DB: Flyway history ==='
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;

\echo '=== ORDER DB: payments important columns ==='
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema='public'
  and table_name='payments'
  and column_name in ('id','order_id','transaction_id','vnp_txn_ref','amount','status','created_at','updated_at')
order by column_name;

\echo '=== ORDER DB: order status constraints ==='
select conname, pg_get_constraintdef(c.oid) as definition
from pg_constraint c
join pg_class t on t.oid = c.conrelid
where t.relname='orders'
  and conname like '%status%'
order by conname;

\echo '=== ORDER DB: order state summary ==='
select status, payment_status, count(*)
from orders
group by status, payment_status
order by status, payment_status;

\echo '=== ORDER DB: payment state summary ==='
select status, count(*)
from payments
group by status
order by status;

\echo '=== ORDER DB: latest orders ==='
select id, buyer_id, seller_id, status, payment_status, total_price, created_at, updated_at
from orders
order by created_at desc
limit 10;

\echo '=== ORDER DB: latest payments ==='
select id, order_id, transaction_id, vnp_txn_ref, amount, status, created_at, updated_at
from payments
order by created_at desc
limit 10;

\echo '=== ORDER DB: pending payment orders ==='
select id, buyer_id, seller_id, status, payment_status, total_price, created_at, updated_at
from orders
where status='PENDING_PAYMENT'
order by created_at desc
limit 10;

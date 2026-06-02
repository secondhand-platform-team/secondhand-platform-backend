\echo '=== CORE DB: Flyway history ==='
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;

\echo '=== CORE DB: item state summary ==='
select status, count(*)
from items
group by status
order by status;

\echo '=== CORE DB: latest items ==='
select item_id, title, price, status, user_id, created_at, updated_at
from items
order by created_at desc
limit 10;

\echo '=== CORE DB: reserved items ==='
select item_id, title, status, reserved_by, reserved_until, updated_at
from items
where status='RESERVED'
order by updated_at desc
limit 10;

\echo '=== CORE DB: catalog counts ==='
select 'categories' as table_name, count(*) from categories
union all
select 'category_attributes', count(*) from category_attributes
union all
select 'items', count(*) from items
union all
select 'notifications', count(*) from notifications;

\echo '=== CORE DB: latest notifications ==='
select id, user_id, type, is_read, created_at
from notifications
order by created_at desc
limit 10;

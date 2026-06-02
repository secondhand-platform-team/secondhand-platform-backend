\echo '=== AUTH DB: Flyway history ==='
select installed_rank, version, description, type, success, installed_on
from flyway_schema_history
order by installed_rank;

\echo '=== AUTH DB: users by role ==='
select role, count(*)
from users
group by role
order by role;

\echo '=== AUTH DB: latest users, no password shown ==='
select id, email, phone_number, role, created_at
from users
order by created_at desc
limit 10;

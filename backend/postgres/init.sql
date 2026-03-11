-- Auto-create secondhand_core_db if it does not exist
SELECT 'CREATE DATABASE secondhand_core_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'secondhand_core_db')\gexec

SELECT 'CREATE DATABASE secondhand_order_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'secondhand_order_db')\gexec

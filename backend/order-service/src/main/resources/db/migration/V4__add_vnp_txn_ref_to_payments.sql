-- Add vnp_txn_ref column to payments table for accurate VNPay reference lookup
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS vnp_txn_ref character varying(255);

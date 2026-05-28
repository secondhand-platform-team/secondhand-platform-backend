-- Align orders.status check constraint with OrderStatus enum values used by the application.

ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE public.orders
    ADD CONSTRAINT orders_status_check CHECK (
        (status)::text = ANY (
            ARRAY[
                'PENDING_PAYMENT'::character varying,
                'PAID'::character varying,
                'PREPARING'::character varying,
                'HANDOVER_TO_SHIPPER'::character varying,
                'IN_TRANSIT'::character varying,
                'DELIVERED'::character varying,
                'RECEIVED'::character varying,
                'COMPLETED'::character varying,
                'CANCELLED'::character varying,
                'DISPUTED'::character varying
            ]::text[]
        )
    );

ALTER TABLE public.notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE public.notifications
    ADD CONSTRAINT notifications_type_check CHECK (
        (type)::text = ANY (
            (ARRAY[
                'ITEM_FAVORITED'::character varying,
                'ITEM_COMMENTED'::character varying,
                'ITEM_REPORTED'::character varying,
                'GIVEAWAY_REQUEST'::character varying,
                'SYSTEM'::character varying,
                'WALLET_DEPOSIT_SUCCESS'::character varying,
                'WALLET_DEDUCTION'::character varying,
                'ORDER_CREATED'::character varying,
                'ORDER_NEW_FOR_SELLER'::character varying,
                'ORDER_PREPARING'::character varying,
                'ORDER_HANDOVER'::character varying,
                'ORDER_IN_TRANSIT'::character varying,
                'ORDER_DELIVERED'::character varying,
                'ORDER_RECEIVED'::character varying,
                'ORDER_COMPLETED'::character varying,
                'ORDER_CANCELLED'::character varying,
                'ORDER_DISPUTED'::character varying,
                'ORDER_DISPUTE_RESOLVED'::character varying,
                'ORDER_AUTO_COMPLETED'::character varying,
                'ORDER_STATUS'::character varying,
                'ESCROW_HOLD'::character varying,
                'ESCROW_RELEASED'::character varying,
                'ESCROW_REFUNDED'::character varying
            ])::text[]
        )
    );

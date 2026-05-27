--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cart_items (
    id character varying(255) NOT NULL,
    item_id character varying(255),
    price double precision,
    quantity integer,
    cart_id character varying(255),
    created_at timestamp(6) without time zone
);


ALTER TABLE public.cart_items OWNER TO postgres;

--
-- Name: carts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.carts (
    id character varying(255) NOT NULL,
    user_id character varying(255)
);


ALTER TABLE public.carts OWNER TO postgres;

--
-- Name: order_events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_events (
    id character varying(255) NOT NULL,
    actor_id character varying(255),
    actor_role character varying(255),
    created_at timestamp(6) without time zone,
    event_type character varying(255),
    metadata text,
    order_id character varying(255),
    snapshot_json text,
    CONSTRAINT order_events_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['ORDER_CREATED'::character varying, 'ORDER_PAID'::character varying, 'ORDER_PREPARING'::character varying, 'ORDER_HANDOVER'::character varying, 'ORDER_IN_TRANSIT'::character varying, 'ORDER_DELIVERED'::character varying, 'ORDER_COMPLETED'::character varying, 'ORDER_AUTO_COMPLETED'::character varying, 'ORDER_CANCELLED'::character varying, 'ORDER_DISPUTED'::character varying, 'ORDER_DISPUTE_RESOLVED'::character varying, 'ESCROW_HELD'::character varying, 'ESCROW_RELEASED'::character varying, 'ESCROW_REFUNDED'::character varying, 'STATUS_UPDATED'::character varying])::text[])))
);


ALTER TABLE public.order_events OWNER TO postgres;

--
-- Name: order_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_history (
    id character varying(255) NOT NULL,
    changed_at timestamp(6) without time zone,
    new_status character varying(255),
    old_status character varying(255),
    order_id character varying(255)
);


ALTER TABLE public.order_history OWNER TO postgres;

--
-- Name: order_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_items (
    id character varying(255) NOT NULL,
    item_id character varying(255),
    item_name character varying(255),
    price double precision,
    quantity integer,
    seller_id character varying(255),
    order_id character varying(255),
    item_image_url text
);


ALTER TABLE public.order_items OWNER TO postgres;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orders (
    id character varying(255) NOT NULL,
    buyer_id character varying(255),
    created_at timestamp(6) without time zone,
    payment_status character varying(255),
    receiver_name character varying(255),
    receiver_phone character varying(255),
    shipping_address character varying(255),
    status character varying(255),
    total_price double precision,
    updated_at timestamp(6) without time zone,
    auto_complete_at timestamp(6) without time zone,
    cancel_reason text,
    dispute_reason text,
    escrow_transaction_id character varying(255),
    seller_id character varying(255),
    CONSTRAINT orders_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[]))),
    CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'PAID'::character varying, 'SHIPPING'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying])::text[])))
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    id character varying(255) NOT NULL,
    amount double precision,
    created_at timestamp(6) without time zone,
    method character varying(255),
    paid_at timestamp(6) without time zone,
    response_code character varying(255),
    secure_hash character varying(255),
    status character varying(255),
    transaction_id character varying(255),
    order_id character varying(255),
    CONSTRAINT payments_method_check CHECK (((method)::text = ANY ((ARRAY['COD'::character varying, 'BANK_TRANSFER'::character varying, 'MOMO'::character varying, 'VNPAY'::character varying])::text[]))),
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- Name: shipments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.shipments (
    id character varying(255) NOT NULL,
    carrier character varying(255),
    delivered_at timestamp(6) without time zone,
    shipped_at timestamp(6) without time zone,
    status character varying(255),
    tracking_code character varying(255),
    order_id character varying(255),
    current_location text,
    estimated_delivery timestamp(6) without time zone
);


ALTER TABLE public.shipments OWNER TO postgres;

--
-- Name: transactions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.transactions (
    id character varying(255) NOT NULL,
    amount double precision,
    created_at timestamp(6) without time zone,
    status character varying(255),
    transaction_code character varying(255),
    payment_id character varying(255),
    CONSTRAINT transactions_status_check CHECK (((status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'PENDING'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.transactions OWNER TO postgres;

--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);


--
-- Name: carts carts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);


--
-- Name: order_events order_events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_events
    ADD CONSTRAINT order_events_pkey PRIMARY KEY (id);


--
-- Name: order_history order_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_history
    ADD CONSTRAINT order_history_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: shipments shipments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT shipments_pkey PRIMARY KEY (id);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: payments uk_8vo36cen604as7etdfwmyjsxt; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uk_8vo36cen604as7etdfwmyjsxt UNIQUE (order_id);


--
-- Name: transactions uk_c58st25t6d4nsanm9anfiktis; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT uk_c58st25t6d4nsanm9anfiktis UNIQUE (payment_id);


--
-- Name: shipments uk_hrhy2yghr8dampg1jtecuekvp; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT uk_hrhy2yghr8dampg1jtecuekvp UNIQUE (order_id);


--
-- Name: idx_order_events_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_order_events_created_at ON public.order_events USING btree (created_at);


--
-- Name: idx_order_events_order_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_order_events_order_id ON public.order_events USING btree (order_id);


--
-- Name: idx_orders_buyer_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_buyer_id ON public.orders USING btree (buyer_id);


--
-- Name: idx_orders_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_created_at ON public.orders USING btree (created_at);


--
-- Name: idx_orders_seller_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_seller_id ON public.orders USING btree (seller_id);


--
-- Name: idx_orders_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_status ON public.orders USING btree (status);


--
-- Name: payments fk81gagumt0r8y3rmudcgpbk42l; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk81gagumt0r8y3rmudcgpbk42l FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: order_items fkbioxgbv59vetrxe0ejfubep1w; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: transactions fkmt44qv8av8abvaqb5nbhjnmi2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fkmt44qv8av8abvaqb5nbhjnmi2 FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: cart_items fkpcttvuq4mxppo8sxggjtn5i2c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fkpcttvuq4mxppo8sxggjtn5i2c FOREIGN KEY (cart_id) REFERENCES public.carts(id);


--
-- Name: shipments fkrnt4wht95lxxplspltrg9681s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT fkrnt4wht95lxxplspltrg9681s FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- PostgreSQL database dump complete
--



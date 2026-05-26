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
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
    category_id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(255),
    name character varying(255),
    posting_fee bigint NOT NULL,
    slug character varying(255),
    updated_at timestamp(6) without time zone,
    parent_id character varying(255)
);


ALTER TABLE public.categories OWNER TO postgres;

--
-- Name: category_attributes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.category_attributes (
    attribute_id character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    data_type character varying(255) NOT NULL,
    description text,
    filterable boolean NOT NULL,
    max_value_number numeric(38,2),
    min_value_number numeric(38,2),
    name character varying(255) NOT NULL,
    options_json text,
    required boolean NOT NULL,
    searchable boolean NOT NULL,
    sort_order integer NOT NULL,
    unit character varying(255),
    updated_at timestamp(6) without time zone,
    category_id character varying(255) NOT NULL,
    CONSTRAINT category_attributes_data_type_check CHECK (((data_type)::text = ANY ((ARRAY['STRING'::character varying, 'NUMBER'::character varying, 'INTEGER'::character varying, 'BOOLEAN'::character varying, 'DATE'::character varying, 'ENUM'::character varying, 'JSON'::character varying])::text[])))
);


ALTER TABLE public.category_attributes OWNER TO postgres;

--
-- Name: favorite_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.favorite_items (
    id character varying(255) NOT NULL,
    user_id character varying(255),
    item_id character varying(255) NOT NULL
);


ALTER TABLE public.favorite_items OWNER TO postgres;

--
-- Name: giveaway_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.giveaway_requests (
    id character varying(255) NOT NULL,
    content character varying(255),
    created_at timestamp(6) without time zone,
    item_id character varying(255) NOT NULL
);


ALTER TABLE public.giveaway_requests OWNER TO postgres;

--
-- Name: item_attribute_values; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.item_attribute_values (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    value_boolean boolean,
    value_date date,
    value_integer bigint,
    value_json text,
    value_number numeric(38,2),
    value_string text,
    attribute_id character varying(255) NOT NULL,
    item_id character varying(255) NOT NULL
);


ALTER TABLE public.item_attribute_values OWNER TO postgres;

--
-- Name: item_images; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.item_images (
    id character varying(255) NOT NULL,
    cloudinary_public_id character varying(255),
    display_order integer,
    is_primary boolean,
    is_thumbnail boolean,
    url character varying(255),
    item_id character varying(255) NOT NULL
);


ALTER TABLE public.item_images OWNER TO postgres;

--
-- Name: items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.items (
    item_id character varying(255) NOT NULL,
    condition character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    description text,
    location text,
    payment_initiated_at timestamp(6) without time zone,
    payment_url text,
    price numeric(38,2),
    status character varying(255),
    title text,
    transaction_id character varying(255),
    transaction_type character varying(255),
    updated_at timestamp(6) without time zone,
    user_id character varying(255),
    view integer,
    category_id character varying(255) NOT NULL,
    reserved_by character varying(255),
    reserved_until timestamp(6) without time zone,
    CONSTRAINT items_condition_check CHECK (((condition)::text = ANY ((ARRAY['NEW'::character varying, 'LIKE_NEW'::character varying, 'USED'::character varying, 'FOR_PARTS'::character varying])::text[]))),
    CONSTRAINT items_status_check CHECK (((status)::text = ANY ((ARRAY['RESERVED'::character varying, 'SOLD'::character varying, 'HIDDEN'::character varying, 'ACTIVE'::character varying, 'DRAFT'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT items_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['SELL'::character varying, 'GIVE_AWAY'::character varying, 'FREE_SELL'::character varying])::text[])))
);


ALTER TABLE public.items OWNER TO postgres;

--
-- Name: locations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.locations (
    location_id character varying(255) NOT NULL,
    address character varying(255),
    city character varying(255),
    district character varying(255),
    ward character varying(255),
    item_id character varying(255)
);


ALTER TABLE public.locations OWNER TO postgres;

--
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id character varying(255) NOT NULL,
    content character varying(255),
    created_at timestamp(6) without time zone,
    is_read boolean,
    type character varying(255),
    user_id character varying(255),
    item_id character varying(255),
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['ITEM_FAVORITED'::character varying, 'ITEM_COMMENTED'::character varying, 'ITEM_REPORTED'::character varying, 'GIVEAWAY_REQUEST'::character varying, 'SYSTEM'::character varying, 'WALLET_DEPOSIT_SUCCESS'::character varying, 'WALLET_DEDUCTION'::character varying])::text[])))
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- Name: report_images; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_images (
    id character varying(255) NOT NULL,
    image_url character varying(255) NOT NULL,
    report_id character varying(255) NOT NULL
);


ALTER TABLE public.report_images OWNER TO postgres;

--
-- Name: reports; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reports (
    id character varying(255) NOT NULL,
    admin_note text,
    assigned_staff_id character varying(255),
    code character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description text,
    reason character varying(255) NOT NULL,
    reporter_id character varying(255) NOT NULL,
    resolved_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    item_id character varying(255) NOT NULL,
    CONSTRAINT reports_code_check CHECK (((code)::text = ANY ((ARRAY['FRAUD'::character varying, 'COUNTERFEIT'::character varying, 'FORBIDDEN'::character varying, 'WRONG_CAT'::character varying, 'SOLD_OUT'::character varying])::text[]))),
    CONSTRAINT reports_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'REVIEWING'::character varying, 'RESOLVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.reports OWNER TO postgres;

--
-- Name: reviews; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reviews (
    review_id character varying(255) NOT NULL,
    comment_content character varying(255),
    created_at timestamp(6) without time zone,
    rating integer,
    reivewer_id character varying(255),
    item_id character varying(255) NOT NULL
);


ALTER TABLE public.reviews OWNER TO postgres;

--
-- Name: search_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.search_history (
    id character varying(255) NOT NULL,
    category_id character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    result_count integer,
    search_query text NOT NULL,
    user_id character varying(255) NOT NULL
);


ALTER TABLE public.search_history OWNER TO postgres;

--
-- Name: view_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.view_history (
    id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    session_id character varying(255),
    user_id character varying(255) NOT NULL,
    viewed_at timestamp(6) without time zone NOT NULL,
    item_id character varying(255) NOT NULL
);


ALTER TABLE public.view_history OWNER TO postgres;

--
-- Name: wallet_transactions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wallet_transactions (
    id character varying(255) NOT NULL,
    amount double precision,
    created_at timestamp(6) without time zone,
    reference_id character varying(255),
    status character varying(255),
    type character varying(255),
    wallet_id character varying(255) NOT NULL,
    CONSTRAINT wallet_transactions_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT wallet_transactions_type_check CHECK (((type)::text = ANY ((ARRAY['DEPOSIT'::character varying, 'WITHDRAW'::character varying, 'PAYMENT'::character varying, 'REFUND'::character varying])::text[])))
);


ALTER TABLE public.wallet_transactions OWNER TO postgres;

--
-- Name: wallets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wallets (
    id character varying(255) NOT NULL,
    balance double precision,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    user_id character varying(255) NOT NULL
);


ALTER TABLE public.wallets OWNER TO postgres;

--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (category_id);


--
-- Name: category_attributes category_attributes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category_attributes
    ADD CONSTRAINT category_attributes_pkey PRIMARY KEY (attribute_id);


--
-- Name: favorite_items favorite_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorite_items
    ADD CONSTRAINT favorite_items_pkey PRIMARY KEY (id);


--
-- Name: giveaway_requests giveaway_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.giveaway_requests
    ADD CONSTRAINT giveaway_requests_pkey PRIMARY KEY (id);


--
-- Name: item_attribute_values item_attribute_values_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.item_attribute_values
    ADD CONSTRAINT item_attribute_values_pkey PRIMARY KEY (id);


--
-- Name: item_images item_images_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.item_images
    ADD CONSTRAINT item_images_pkey PRIMARY KEY (id);


--
-- Name: items items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT items_pkey PRIMARY KEY (item_id);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (location_id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: report_images report_images_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_images
    ADD CONSTRAINT report_images_pkey PRIMARY KEY (id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (review_id);


--
-- Name: search_history search_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.search_history
    ADD CONSTRAINT search_history_pkey PRIMARY KEY (id);


--
-- Name: category_attributes uk6ux76r9nm7g4fgn5mkdv8sqn0; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category_attributes
    ADD CONSTRAINT uk6ux76r9nm7g4fgn5mkdv8sqn0 UNIQUE (category_id, code);


--
-- Name: locations uk_5e2ck8v68rb3f7mtyu5xa6dkf; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT uk_5e2ck8v68rb3f7mtyu5xa6dkf UNIQUE (item_id);


--
-- Name: categories uk_oul14ho7bctbefv8jywp5v3i2; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT uk_oul14ho7bctbefv8jywp5v3i2 UNIQUE (slug);


--
-- Name: wallets uk_sswfdl9fq40xlkove1y5kc7kv; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT uk_sswfdl9fq40xlkove1y5kc7kv UNIQUE (user_id);


--
-- Name: item_attribute_values uke2yek04yb7ymw4uu81hd0cfrc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.item_attribute_values
    ADD CONSTRAINT uke2yek04yb7ymw4uu81hd0cfrc UNIQUE (item_id, attribute_id);


--
-- Name: view_history view_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.view_history
    ADD CONSTRAINT view_history_pkey PRIMARY KEY (id);


--
-- Name: wallet_transactions wallet_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT wallet_transactions_pkey PRIMARY KEY (id);


--
-- Name: wallets wallets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_pkey PRIMARY KEY (id);


--
-- Name: idx_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_created_at ON public.search_history USING btree (created_at DESC);


--
-- Name: idx_items_category_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_items_category_id ON public.items USING btree (category_id);


--
-- Name: idx_items_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_items_created_at ON public.items USING btree (created_at);


--
-- Name: idx_items_price; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_items_price ON public.items USING btree (price);


--
-- Name: idx_items_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_items_status ON public.items USING btree (status);


--
-- Name: idx_items_status_deleted; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_items_status_deleted ON public.items USING btree (status, deleted_at);


--
-- Name: idx_items_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_items_user_id ON public.items USING btree (user_id);


--
-- Name: idx_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_id ON public.search_history USING btree (user_id);


--
-- Name: idx_user_id_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_id_created_at ON public.search_history USING btree (user_id DESC, created_at DESC);


--
-- Name: idx_vh_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vh_created_at ON public.view_history USING btree (created_at DESC);


--
-- Name: idx_vh_item_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vh_item_id ON public.view_history USING btree (item_id);


--
-- Name: idx_vh_item_id_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vh_item_id_created_at ON public.view_history USING btree (item_id, created_at DESC);


--
-- Name: idx_vh_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vh_user_id ON public.view_history USING btree (user_id);


--
-- Name: idx_vh_user_id_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vh_user_id_created_at ON public.view_history USING btree (user_id DESC, created_at DESC);


--
-- Name: idx_vh_user_id_item_id_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_vh_user_id_item_id_created_at ON public.view_history USING btree (user_id DESC, item_id, created_at DESC);


--
-- Name: reports fk2kmyy6fyd3ho2xdc7nn75milx; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fk2kmyy6fyd3ho2xdc7nn75milx FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: view_history fk2ua8ef15cvus5taytophikdhy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.view_history
    ADD CONSTRAINT fk2ua8ef15cvus5taytophikdhy FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: item_images fk31vykiuqi6nfw2rmvw37qlydy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.item_images
    ADD CONSTRAINT fk31vykiuqi6nfw2rmvw37qlydy FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: wallet_transactions fk8seu7b87ifqi09ghhssusmb0x; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT fk8seu7b87ifqi09ghhssusmb0x FOREIGN KEY (wallet_id) REFERENCES public.wallets(id);


--
-- Name: item_attribute_values fk9r1bgxdtn3tsj318qodngpw5p; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.item_attribute_values
    ADD CONSTRAINT fk9r1bgxdtn3tsj318qodngpw5p FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: item_attribute_values fk9ufpe8bhytf1iv2duf752m2qc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.item_attribute_values
    ADD CONSTRAINT fk9ufpe8bhytf1iv2duf752m2qc FOREIGN KEY (attribute_id) REFERENCES public.category_attributes(attribute_id);


--
-- Name: giveaway_requests fkc8p9riierxnwmb73wpu7pay3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.giveaway_requests
    ADD CONSTRAINT fkc8p9riierxnwmb73wpu7pay3 FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: reviews fkd0qivr20lp2u34cfcrr0ibct7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fkd0qivr20lp2u34cfcrr0ibct7 FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: report_images fkio33xl5nyhe7fv6e8me83ddj5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_images
    ADD CONSTRAINT fkio33xl5nyhe7fv6e8me83ddj5 FOREIGN KEY (report_id) REFERENCES public.reports(id);


--
-- Name: items fkjcdcde7htb3tyjgouo4g9xbmr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT fkjcdcde7htb3tyjgouo4g9xbmr FOREIGN KEY (category_id) REFERENCES public.categories(category_id);


--
-- Name: favorite_items fkkrdaj0l52ix1dcc7se7ko5f0c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorite_items
    ADD CONSTRAINT fkkrdaj0l52ix1dcc7se7ko5f0c FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: notifications fkoaluyac5x5cfeqtmbyffrfuc8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkoaluyac5x5cfeqtmbyffrfuc8 FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: locations fkps37grxxuw0k0h4t4pc0ylakx; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT fkps37grxxuw0k0h4t4pc0ylakx FOREIGN KEY (item_id) REFERENCES public.items(item_id);


--
-- Name: category_attributes fks8x8saggy3b3wx581pn813por; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.category_attributes
    ADD CONSTRAINT fks8x8saggy3b3wx581pn813por FOREIGN KEY (category_id) REFERENCES public.categories(category_id);


--
-- Name: categories fksaok720gsu4u2wrgbk10b5n8d; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fksaok720gsu4u2wrgbk10b5n8d FOREIGN KEY (parent_id) REFERENCES public.categories(category_id);


--
-- PostgreSQL database dump complete
--



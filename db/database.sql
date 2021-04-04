--
-- PostgreSQL database dump
--

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

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: applications; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.applications (
    id integer NOT NULL,
    name character varying(50)
);


ALTER TABLE public.applications OWNER TO lorans;

--
-- Name: applications_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.applications_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.applications_id_seq OWNER TO lorans;

--
-- Name: applications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.applications_id_seq OWNED BY public.applications.id;


--
-- Name: aps; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.aps (
    id character varying(130) NOT NULL,
    protocol_ver character varying(50),
    max_power integer,
    channels_num integer,
    duty_cycle_refresh time without time zone,
    lora_protocol character varying(50),
    lora_protocol_ver character varying(50),
    transmission_param_id integer NOT NULL,
    stat_model json DEFAULT '[{"sf": 7, "pw": 10, "rw": 1}, {"sf": 7, "pw": 14, "rw": 1}, {"sf": 8, "pw": 10, "rw": 1}, {"sf": 8, "pw": 14, "rw": 1}, {"sf": 9, "pw": 10, "rw": 1}, {"sf": 9, "pw": 14, "rw": 1}, {"sf": 10, "pw": 10, "rw": 1},  {"sf": 10, "pw": 14, "rw": 1}, {"sf": 11, "pw": 10, "rw": 1}, {"sf": 11, "pw": 14, "rw": 1}, {"sf": 12, "pw": 10, "rw": 1}, {"sf": 12, "pw": 14, "rw": 1}]'::json
);


ALTER TABLE public.aps OWNER TO lorans;

--
-- Name: downlink_messages; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.downlink_messages (
    id integer NOT NULL,
    app_data character varying(1024),
    net_data json,
    duty_cycle_remaining integer,
    sent boolean,
    ack_required boolean,
    delivered boolean,
    send_time timestamp without time zone,
    frequency numeric,
    spf integer,
    power integer,
    airtime integer,
    coderate character varying(20),
    bandwidth integer,
    ap_id character varying(130),
    node_id character varying(130) NOT NULL
);


ALTER TABLE public.downlink_messages OWNER TO lorans;

--
-- Name: downlink_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.downlink_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.downlink_messages_id_seq OWNER TO lorans;

--
-- Name: downlink_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.downlink_messages_id_seq OWNED BY public.downlink_messages.id;


--
-- Name: message_types; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.message_types (
    id integer NOT NULL,
    name character varying(50)
);


ALTER TABLE public.message_types OWNER TO lorans;

--
-- Name: message_types_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.message_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.message_types_id_seq OWNER TO lorans;

--
-- Name: message_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.message_types_id_seq OWNED BY public.message_types.id;


--
-- Name: nodes; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.nodes (
    id character varying(130) NOT NULL,
    dh_key character varying(128),
    last_seq integer DEFAULT 1,
    upstream_power integer,
    downstream_power integer,
    spf integer,
    duty_cycle_refresh time without time zone,
    application_id integer NOT NULL,
    transmission_param_id integer NOT NULL,
    stat_model json DEFAULT '[{"sf": 7, "pw": 10, "rw": 1}, {"sf": 7, "pw": 14, "rw": 1}, {"sf": 8, "pw": 10, "rw": 1}, {"sf": 8, "pw": 14, "rw": 1}, {"sf": 9, "pw": 10, "rw": 1}, {"sf": 9, "pw": 14, "rw": 1}, {"sf": 10, "pw": 10, "rw": 1},  {"sf": 10, "pw": 14, "rw": 1}, {"sf": 11, "pw": 10, "rw": 1}, {"sf": 11, "pw": 14, "rw": 1}, {"sf": 12, "pw": 10, "rw": 1}, {"sf": 12, "pw": 14, "rw": 1}]'::json
);


ALTER TABLE public.nodes OWNER TO lorans;

--
-- Name: transmission_params; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.transmission_params (
    id integer NOT NULL,
    registration_freq numeric[],
    emergency_freq numeric[],
    standard_freq numeric[],
    coderate character varying(20),
    bandwidth integer
);


ALTER TABLE public.transmission_params OWNER TO lorans;

--
-- Name: transmission_params_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.transmission_params_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.transmission_params_id_seq OWNER TO lorans;

--
-- Name: transmission_params_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.transmission_params_id_seq OWNED BY public.transmission_params.id;


--
-- Name: uplink_messages; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.uplink_messages (
    id integer NOT NULL,
    app_data character varying(1024),
    snr numeric,
    rssi numeric,
    duty_cycle_remaining integer,
    is_primary boolean,
    receive_time timestamp without time zone,
    seq integer,
    frequency numeric,
    spf integer,
    power integer,
    airtime integer,
    coderate character varying(20),
    bandwidth integer,
    msg_group_number integer,
    message_type_id integer NOT NULL,
    ap_id character varying(130) NOT NULL,
    node_id character varying(130) NOT NULL
);


ALTER TABLE public.uplink_messages OWNER TO lorans;

--
-- Name: uplink_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.uplink_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.uplink_messages_id_seq OWNER TO lorans;

--
-- Name: uplink_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.uplink_messages_id_seq OWNED BY public.uplink_messages.id;


--
-- Name: applications id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.applications ALTER COLUMN id SET DEFAULT nextval('public.applications_id_seq'::regclass);


--
-- Name: downlink_messages id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.downlink_messages ALTER COLUMN id SET DEFAULT nextval('public.downlink_messages_id_seq'::regclass);


--
-- Name: message_types id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.message_types ALTER COLUMN id SET DEFAULT nextval('public.message_types_id_seq'::regclass);


--
-- Name: transmission_params id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.transmission_params ALTER COLUMN id SET DEFAULT nextval('public.transmission_params_id_seq'::regclass);


--
-- Name: uplink_messages id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.uplink_messages ALTER COLUMN id SET DEFAULT nextval('public.uplink_messages_id_seq'::regclass);


--
-- Data for Name: applications; Type: TABLE DATA; Schema: public; Owner: lorans
--

COPY public.applications (id, name) FROM stdin;
1	LoRaBand
\.

    channels_num integer,
    duty_cycle_refresh time without time zone,
    lora_protocol character varying(50),
    lora_protocol_ver character varying(50),
    transmission_param_id integer NOT NULL,
    stat_model json DEFAULT '[{"sf": 7, "pw": 10, "rw": 1}, {"sf": 7, "pw": 14, "rw": 1}, {"sf": 8, "pw": 10, "rw": 1}, {"sf": 8, "pw": 14, "rw": 1}, {"sf": 9, "pw": 10, "rw": 1}, {"sf": 9, "pw": 14, "rw": 1}, {"sf": 10, "pw": 10, "rw": 1},  {"sf": 10, "pw": 14, "rw": 1}, {"sf": 11, "pw": 10, "rw": 1}, {"sf": 11, "pw": 14, "rw": 1}, {"sf": 12, "pw": 10, "rw": 1}, {"sf": 12, "pw": 14, "rw": 1}]'::json
);


ALTER TABLE public.aps OWNER TO lorans;

--
-- Name: downlink_messages; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.downlink_messages (
    id integer NOT NULL,
    app_data character varying(1024),
    net_data json,
    duty_cycle_remaining integer,
    sent boolean,
    ack_required boolean,
    delivered boolean,
    send_time timestamp without time zone,
    frequency numeric,
    spf integer,
    power integer,
    airtime integer,
    coderate character varying(20),
    bandwidth integer,
    ap_id character varying(130),
    node_id character varying(130) NOT NULL
);


ALTER TABLE public.downlink_messages OWNER TO lorans;

--
-- Name: downlink_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.downlink_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.downlink_messages_id_seq OWNER TO lorans;

--
-- Name: downlink_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.downlink_messages_id_seq OWNED BY public.downlink_messages.id;


--
-- Name: message_types; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.message_types (
    id integer NOT NULL,
    name character varying(50)
);


ALTER TABLE public.message_types OWNER TO lorans;

--
-- Name: message_types_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.message_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.message_types_id_seq OWNER TO lorans;

--
-- Name: message_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.message_types_id_seq OWNED BY public.message_types.id;


--
-- Name: nodes; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.nodes (
    id character varying(130) NOT NULL,
    dh_key character varying(128),
    last_seq integer DEFAULT 1,
    upstream_power integer,
    downstream_power integer,
    spf integer,
    duty_cycle_refresh time without time zone,
    application_id integer NOT NULL,
    transmission_param_id integer NOT NULL,
    stat_model json DEFAULT '[{"sf": 7, "pw": 10, "rw": 1}, {"sf": 7, "pw": 14, "rw": 1}, {"sf": 8, "pw": 10, "rw": 1}, {"sf": 8, "pw": 14, "rw": 1}, {"sf": 9, "pw": 10, "rw": 1}, {"sf": 9, "pw": 14, "rw": 1}, {"sf": 10, "pw": 10, "rw": 1},  {"sf": 10, "pw": 14, "rw": 1}, {"sf": 11, "pw": 10, "rw": 1}, {"sf": 11, "pw": 14, "rw": 1}, {"sf": 12, "pw": 10, "rw": 1}, {"sf": 12, "pw": 14, "rw": 1}]'::json
);


ALTER TABLE public.nodes OWNER TO lorans;

--
-- Name: transmission_params; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.transmission_params (
    id integer NOT NULL,
    registration_freq numeric[],
    emergency_freq numeric[],
    standard_freq numeric[],
    coderate character varying(20),
    bandwidth integer
);


ALTER TABLE public.transmission_params OWNER TO lorans;

--
-- Name: transmission_params_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.transmission_params_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.transmission_params_id_seq OWNER TO lorans;

--
-- Name: transmission_params_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.transmission_params_id_seq OWNED BY public.transmission_params.id;


--
-- Name: uplink_messages; Type: TABLE; Schema: public; Owner: lorans
--

CREATE TABLE public.uplink_messages (
    id integer NOT NULL,
    app_data character varying(1024),
    snr numeric,
    rssi numeric,
    duty_cycle_remaining integer,
    is_primary boolean,
    receive_time timestamp without time zone,
    seq integer,
    frequency numeric,
    spf integer,
    power integer,
    airtime integer,
    coderate character varying(20),
    bandwidth integer,
    msg_group_number integer,
    message_type_id integer NOT NULL,
    ap_id character varying(130) NOT NULL,
    node_id character varying(130) NOT NULL
);


ALTER TABLE public.uplink_messages OWNER TO lorans;

--
-- Name: uplink_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: lorans
--

CREATE SEQUENCE public.uplink_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.uplink_messages_id_seq OWNER TO lorans;

--
-- Name: uplink_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: lorans
--

ALTER SEQUENCE public.uplink_messages_id_seq OWNED BY public.uplink_messages.id;


--
-- Name: applications id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.applications ALTER COLUMN id SET DEFAULT nextval('public.applications_id_seq'::regclass);


--
-- Name: downlink_messages id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.downlink_messages ALTER COLUMN id SET DEFAULT nextval('public.downlink_messages_id_seq'::regclass);


--
-- Name: message_types id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.message_types ALTER COLUMN id SET DEFAULT nextval('public.message_types_id_seq'::regclass);


--
-- Name: transmission_params id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.transmission_params ALTER COLUMN id SET DEFAULT nextval('public.transmission_params_id_seq'::regclass);


--
-- Name: uplink_messages id; Type: DEFAULT; Schema: public; Owner: lorans
--

ALTER TABLE ONLY public.uplink_messages ALTER COLUMN id SET DEFAULT nextval('public.uplink_messages_id_seq'::regclass);
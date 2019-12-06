﻿/* Database, tables, user SQL dump */

CREATE DATABASE lorafiit
  WITH OWNER = postgres
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       LC_COLLATE = 'Slovak_Slovakia.1250'
       LC_CTYPE = 'Slovak_Slovakia.1250'
       CONNECTION LIMIT = -1;

CREATE TABLE applications
(
id SERIAL PRIMARY KEY,
name VARCHAR(50)
);

CREATE TABLE transmission_params
(
id SERIAL PRIMARY KEY,
registration_freq DECIMAL[],
emergency_freq DECIMAL[],
standard_freq DECIMAL[],
coderate VARCHAR(20),
bandwidth INT
);

/* vo formate 00:MM:SS, urcije ktoru minutu a sekundu kazdej hodiny sa ma obnovit duty cycle na danom zariadeni */
CREATE TABLE nodes
(
id VARCHAR(130) PRIMARY KEY,
dh_key VARCHAR(128),
last_seq INT,
upstream_power INT,
downstream_Power INT,
spf INT,
duty_cycle_refresh TIME,
application_id int REFERENCES applications(id) NOT NULL,
transmission_param_id int REFERENCES transmission_params(id) NOT NULL
);


CREATE TABLE aps
(
id VARCHAR(130) PRIMARY KEY,
protocol_ver VARCHAR(50),
max_power INT,
channels_num INT,
duty_cycle_refresh TIME,
lora_protocol VARCHAR(50),
lora_protocol_ver VARCHAR(50),
transmission_param_id int REFERENCES transmission_params(id) NOT NULL
);

CREATE TABLE uplink_messages
(
id SERIAL PRIMARY KEY,
app_data VARCHAR(1024),
snr DECIMAL,
rssi DECIMAL,
duty_cycle_remaining INT,
is_primary BOOLEAN,
receive_time TIMESTAMP,
msg_group_number INT,
ap_id VARCHAR(130) REFERENCES aps(id) NOT NULL,
node_id VARCHAR(130) REFERENCES nodes(id) NOT NULL
);

CREATE TABLE downlink_messages
(
id SERIAL PRIMARY KEY,
app_data VARCHAR(1024),
net_data JSON,
duty_cycle_remaining INT,
sent BOOLEAN,
ack_required BOOLEAN,
delivered BOOLEAN,
send_time TIMESTAMP,
ap_id VARCHAR(130) REFERENCES aps(id),
node_id VARCHAR(130) REFERENCES nodes(id) NOT NULL
);
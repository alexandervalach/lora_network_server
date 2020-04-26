-- noinspection SqlNoDataSourceInspectionForFile

-- noinspection SqlDialectInspectionForFile

/* Table SQL dump */

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

/* TIME vo formate 00:MM:SS, urcije ktoru minutu a sekundu kazdej hodiny sa ma obnovit duty cycle na danom zariadeni */
CREATE TABLE nodes	
(
id VARCHAR(130) PRIMARY KEY,
dh_key VARCHAR(128),
last_seq INT DEFAULT 1,
upstream_power INT,
downstream_Power INT,
spf INT,
duty_cycle_refresh TIME,
stat_model JSON,
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
stat_model JSON,
transmission_param_id INT REFERENCES transmission_params(id) NOT NULL
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
seq INT,
frequency DECIMAL,
spf INT,
power INT,
airtime INT,
coderate VARCHAR(20),
bandwidth INT,
msg_group_number INT,
message_type_id INT REFERENCES message_types(id) NOT NULL,
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
frequency DECIMAL,
spf INT,
power INT,
airtime INT,
coderate VARCHAR(20),
bandwidth INT,
ap_id VARCHAR(130) REFERENCES aps(id),
node_id VARCHAR(130) REFERENCES nodes(id) NOT NULL
);

CREATE TABLE message_types
(
id SERIAL PRIMARY KEY,
name VARCHAR(50)
);

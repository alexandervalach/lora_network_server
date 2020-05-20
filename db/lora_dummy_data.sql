/* Fill the database with dummy data */

INSERT INTO applications (name) VALUES
('Hlavna appka'),
('Dalsia Appka'),
('Testovacia');

INSERT INTO transmission_params
(registration_freq, emergency_freq, standard_freq, coderate, bandwidth) VALUES
('{866700000}','{866900000}','{866100000,866300000,866500000}','4/5',125000),
('{866700000}','{866900000}','{866100000,866300000,866500000}','4/5',125000);


INSERT INTO nodes
(id, dh_key, last_seq, upstream_power, downstream_power, spf, duty_cycle_refresh, application_id, transmission_param_id,stat_model) VALUES
('11111aaa','xababvsfjv12345',20,4,7,12,'00:05:06',1,1,'[{"sf":"7","pw":"5","rw":"8"}]'),
('22222bbb','wwwwwwwwwwwwwww54321',40,4,7,12,'00:32:21',1,1,'[{"sf":"7","pw":"5","rw":"8"}]');

INSERT INTO aps
(id, protocol_ver, max_power, channels_num, duty_cycle_refresh, lora_protocol, lora_protocol_ver, transmission_param_id) VALUES
('99999ff','STIOT v 1.01',8,20,'00:05:06','LoRa@FIIT','0.1a',1),
('88888xx','STIOT v 1.01',8,20,'00:05:06','LoRa@FIIT','0.1a',1);

INSERT INTO uplink_messages
(app_data, snr, rssi, duty_cycle_remaining, is_primary, receive_time, msg_group_number, ap_id, node_id) VALUES
('sdkabgjsjvbajdckjabfhbalkfjhvbrvw',12.5,21,1203,true,'2017-2-24 10:23:54',1,'99999ff','11111aaa'),
('sdkabgjsjvbajdckjabfhbalkfjhvbrvw',1.5,1,1203,false,'2017-2-24 10:23:56',1,'88888xx','11111aaa'),
('ten druhy more',13.5,31,0,false,'2017-2-24 10:23:54',1,'99999ff','22222bbb'),
('ten druhy more',2.5,12,0,true,'2017-2-24 10:23:56',1,'88888xx','22222bbb');

INSERT INTO downlink_messages
(app_data, duty_cycle_remaining, sent, ack_required, delivered, send_time, ap_id, node_id) VALUES
('dole ide nepotvrdzujeme',1203,true,false,true,'2017-2-24 11:23:54','99999ff','11111aaa'),
('dole ide potvrdzujeme',1002,true,true,true,'2017-2-24 11:21:24','88888xx','11111aaa');

INSERT INTO message_types
(id, name) VALUES
(1, 'data'),
(2, 'emergency'),
(3, 'hello'),
(4, 'register');
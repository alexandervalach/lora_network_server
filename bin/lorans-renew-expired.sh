#!/bin/bash
# letsencrypt certificate are only available for root users #onlyROOTS
sudo su

# enter the directory for your server identified by domain name
## cd /etc/letsencrypt/live/domain/

# first export key with custom password
## openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out pkcs.p12 -name lora

# import key to keystore file using custom password
## keytool -importkeystore -destkeystore keystore.jks -srcstoretype PKCS12 -srckeystore pkcs.p12

# copy keystore to network server home folder
## cp keystore.jks /opt/lorans/

# assign rights for lora-network-server
## sudo chown -R :lorans /opt/lorans/keystore.jks

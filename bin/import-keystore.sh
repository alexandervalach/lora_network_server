#!/bin/bash

appdir=/opt/lones
keystore=lones.jks
domain=domain
cert_home="/etc/letsencrypt/live/${domain}/"
user_group=user:group
src_pass=lones
dst_pass=lones

# enter the directory for your server identified by domain name
cd ${cert_home}

# first export key with custom password
# Use this command if you want to take a private key (domain.key) and a certificate (domain.crt)
# and combine them into a PKCS12 file (domain.pfx):
openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out pkcs.p12 -passout pass:"${src_pass}" -name ${domain}

# import key to keystore file using custom password
keytool -importkeystore -destkeystore ${keystore} -srcstoretype PKCS12 -srckeystore pkcs.p12 -srcstorepass ${src_pass} -deststorepass ${dst_pass} -noprompt

# copy keystore to network server home folder
cp ${keystore} ${appdir}

# assign rights for lora-network-server
sudo chown -R ${user_group} ${appdir}/${keystore}

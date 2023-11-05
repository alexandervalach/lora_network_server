# LoRa Network Server

Project aimed at creating an open-source server capable of processing LoRaWAN and LoRa@FIIT messages. This repository provides a Docker Compose configuration for running a PostgreSQL database, pgAdmin, and a custom server application in isolated containers. It's a convenient way to set up a development or testing environment with these services.

**Note:** This repository contains a LoRa Network Server with a primary focus on the LoRa@FIIT link-layer protocol.

**Original Authors:** Karol Cagáň, Ondrej Perešíni, and Alexander Valach

## Prerequisites

Before you begin, make sure you have the following tools and dependencies installed on your system:

- Docker
- Docker Compose

## Getting Started

1. **Clone this repository to your local machine:**

   ```bash
   git clone https://github.com/loraalex/LoNES.git
   ```

2. **Navigate to the project directory:**

   ```bash
   cd LoNES
   ```

3. **Generate a TLS Certificate (before creating the `.env` file):**

   - Replace `${domain}` with your domain name.
   - Run the following commands in a terminal:

     ```bash
     # Install Certbot (Let's Encrypt client) if not already installed
     sudo apt-get update
     sudo apt-get install certbot

     # Obtain a TLS certificate (replace placeholders with your actual domain and email)
     sudo certbot certonly --standalone -d ${domain} --non-interactive --agree-tos --email your@email.com
     ```

   - The certificate and private key will be saved to `/etc/letsencrypt/live/${domain}/`.

4. **Run the `install.sh` script for the initial setup:**

   ```bash
   chmod +x install.sh
   ./install.sh
   ```

   - The `install.sh` script will execute Docker Compose and initialize the database for the first time.

5. **Create a `.env` file in the project directory with the following environment variables (example values provided):**

   ```env
   DB_NAME=lones
   DB_USER=lones
   DB_PASSWORD=(Choose a strong, complex password)
   DB_PORT=5432
   DB_HOST=postgres
   DB_DRIVER=org.postgresql.Driver
   DB_PROTOCOL=jdbc:postgresql

   PGADMIN_EMAIL=lora@fiit.stuba.sk
   PGADMIN_PASSWORD=(Choose a strong, complex password)
   PGADMIN_PORT=5050

   LONES_PORT=8002
   LONES_KEYSTORE=lones.jks
   LONES_KEYSTORE_PASSWORD=(Choose a strong, complex password)
   LONES_PRESHARED_KEY=(e.g. 33 Bytes long key)

   LONES_LOGFILE=logs.log
   ```

6. **Export the Certificate to a Java Key Store (as described in Step 3).**

7. **Start the services using Docker Compose (for subsequent builds):**

   ```bash
   docker-compose up -d
   ```

The services will be running in the background, and you can access them as needed.

## Services

### PostgreSQL

- **Container Name:** postgres_container
- **Image:** postgres:14
- **Environment Variables:** (As specified in your `.env`)
- **Volumes:** postgres:/data/postgres
- **Ports:** DB_PORT:5432 (as defined in your `.env`)
- **Network:** postgres
- **Restart Policy:** always

### pgAdmin

- **Container Name:** pgadmin_container
- **Image:** dpage/pgadmin4:6.20
- **Environment Variables:** (As specified in your `.env`)
- **Volumes:** pgadmin:/var/lib/pgadmin
- **Ports:** PGADMIN_PORT:80 (as defined in your `.env`)
- **Network:** postgres
- **Restart Policy:** always

### Custom Server (LoRa Network Server - lones)

- **Container Name:** lones_container
- **Image:** Custom
- **Dockerfile:** Based on provided Dockerfile
- **Exposed Port:** LONES_PORT (as defined in your `.env`)

### Network

- **Network Name:** postgres
- **Driver:** bridge

### Volumes

- **Volume Name:** postgres
- **Volume Name:** pgadmin
- **Volume Name:** lones

## Additional Information

- The `.env` file should be configured with the necessary environment variables for your services. Ensure that you choose strong, complex passwords for database access, pgAdmin, and the keystore.

- The `LONES_PRESHARED_KEY` should be generated securely and should be of a specific length. Describe the length and generation process according to your security requirements.

#!/bin/bash

docker-compose up -d

# Run the init-db.sh script
./bin/db-init.sh

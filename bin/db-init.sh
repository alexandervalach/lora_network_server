#!/bin/bash

# Set default values for database name and user
DB_NAME="lones"
DB_USER="lones"

# Function to check if the PostgreSQL container is running
is_postgres_container_running() {
  if docker ps --format "{{.Names}}" | grep -q "postgres_container"; then
    return 0
  else
    return 1
  fi
}

# Check if the PostgreSQL container is running
if is_postgres_container_running; then
  # Copy SQL script to the PostgreSQL container
  docker cp ./db/db_tables_with_dummy_data.sql postgres_container:/

  # Execute the SQL script with the provided database name and user
  docker exec -it postgres_container bash -c "psql -d $DB_NAME -U $DB_USER -f db_tables_with_dummy_data.sql"
else
  echo "PostgreSQL container is not running. Please start the PostgreSQL container first."
fi

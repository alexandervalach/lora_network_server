#!/bin/bash

docker cp ../db/db_tables_with_dummy_data.sql postgres_container:/

docker exec -it postgres_container bash -c 'psql -d lones -U lones -f db_tables_with_dummy_data.sql;'

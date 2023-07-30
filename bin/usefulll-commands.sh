# Docker
docker-compose build --no-cache

docker-compose down --volumes
docker-compose down --remove-orphans
docker-compose down --rmi all

docker-compose up --build

docker system prune -a
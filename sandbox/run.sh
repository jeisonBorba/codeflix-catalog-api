# Create docker networks
docker network create elastic
docker network create kafka

# Create docker volumes
docker volume create es01
docker volume create kafka01
docker volume create kconnect01

# Run docker containers
docker compose -f elk/docker-compose.yml up -d elasticsearch
docker compose -f kafka/docker-compose.yml up -d
docker compose -f catalogo_services/docker-compose.yml up -d



# NOTE: execute the following query to enable root connection
# (IT'S ONLY FOR TESTING PURPOSE) we should create a unique user for kafka connect
# ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
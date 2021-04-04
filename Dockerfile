FROM openjdk:11

WORKDIR /app

COPY network-server.jar ./

# Update
RUN apt-get update

# Expose STIoT port
EXPOSE 25001

# Expose docker port for reverse proxy
EXPOSE 3333

# Make installing script executable
# CMD chmod +x install_service.sh

# Install as a service
# CMD ./install_service.sh

# Copy a configuration file
RUN mkdir ./resources
COPY resources/configuration.config ./resources/configuration.config

# Add keystore file
RUN mv keystore.jks ./

# Crate logs file
RUN mkdir ./logs
RUN touch ./logs/logs.log

# Run JAR application
CMD ["java", "-jar", "/app/network-server.jar"]
FROM openjdk:11

RUN mkdir /opt/lones
WORKDIR /opt/lones

COPY . ./

# Expose STIoT port
EXPOSE 8002

RUN mv ./resources/.configuration.config ./resources/configuration.config

# Crate logs file
RUN mkdir ./logs
RUN touch ./logs/logs.log

COPY lones.jks ./

#COPY lones.jar ./

# CMD chmod +x install_service.sh
RUN javac -cp lib/org.json.jar:lib/postgresql-42.2.8.jar -d out ./src/*/*.java

# Produce JAR file
CMD ["jar", "cfm", "/opt/lones/lones.jar", "src/META-INF/MANIFEST.MF", "-C", "out", "."]

# Run JAR application
ENTRYPOINT ["java", "-jar", "lones.jar"]

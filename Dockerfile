COPY . ./

# Expose the port if needed
# EXPOSE 8002

# Optionally, rename the configuration file if needed
# RUN mv ./resources/.configuration.config ./resources/configuration.config

# Create a directory for logs and an empty log file
RUN mkdir ./logs && touch ./logs/logs.log

# Copy any necessary files, such as a keystore
COPY lones.jks ./

# Compile your Java source code
RUN javac -cp lib/org.json.jar:lib/postgresql-42.2.8.jar -d out ./src/*/*.java

# Create the JAR file from compiled classes and a manifest file
RUN jar cfm /opt/lones/lones.jar src/META-INF/MANIFEST.MF -C out .

# (Optional) Clean up any intermediate files if needed
# RUN rm -rf out

# Run the JAR application when the container starts
CMD ["java", "-jar", "lones.jar"]

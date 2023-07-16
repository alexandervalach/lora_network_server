#!/bin/bash

jarfile=network-server.jar
# BUILD SECTION
# Compile the .java files and include external libraries
#
# Huge thanks for https://www.baeldung.com/java-compile-multiple-files for providing necessary explanation
# -cp, Using the classpath, we can define a set of directories or files such as *.jar, *.zip that our source code depends on during compilation. Alternatively, we can set the CLASSPATH environment variable.
# -cp, We should note that the classpath option has higher precedence than the environment variable.
# -cp, If none of them are specified, then the classpath is assumed to be the current directory. When we wish to specify multiple directories, the path separator is ‘:‘ for most operating systems except Windows, where it's ‘;‘.
#
# -sourcepath, This option makes it possible to specify the top directory where all of our source code that needs compilation resides.
# -sourcepath, If not specified, the classpath gets scanned for the sources.
#
# -d, We use this option when we want to have all compiled results in one place, separate from the source code. We need to keep in mind that the path we want to specify must exist beforehand.
# -d, During compilation, this path is used as a root directory, and sub-folders are created automatically according to the package structure of the classes. If this option is not specified, every single *.class file is written next to their corresponding source code *.java file.
#
javac -cp lib/org.json.jar:lib/postgresql-42.2.8.jar -d out src/*/*.java

# Build artifacts and create Java archive
jar cfm ${jarfile} src/META-INF/MANIFEST.MF -C out .

# You can run the application on order to test the build
# However, for production environment, it is recommended to run install-service.sh after successful build
# java -jar network-server.jar

# DEPLOY SECTION
# Deploy Remotely using SCP
# Change the values
rhost=localhost
username=lorafiit
rfolder=/data/lora-network-server

scp ${jarfile} ${username}@${rhost}:/home/${username}

# Restart the system service
ssh ${username}@${rhost} "sudo cp ${jarfile} ${rfolder}"

# Restart the system service
ssh ${username}@${rhost} "sudo systemctl restart lorans"
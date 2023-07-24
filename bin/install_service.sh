#!/bin/sh

# Check root user
if [ `id -u` != 0 ]; then
    echo "ERROR: Not root user?"
    exit 1
fi

echo "Creating launch bash script..."
# Create a bash script that would run jar file
echo "#!/bin/sh" > ./network-server.sh
echo "/usr/bin/java -jar network-server.jar" >> ./network-server.sh
echo "" >> ./network-server.sh 
chmod 777 ./network-server.sh

# Install as a system service
echo "Installing JAR application as a system service..."
touch /lib/systemd/system/lorans.service
echo "[Unit]" > /lib/systemd/system/lorans.service
echo "Description=LoRa Network Server" >> /lib/systemd/system/lorans.service
echo "[Service]" >> /lib/systemd/system/lorans.service
echo "User=`echo $USER`" >> /lib/systemd/system/lorans.service
echo "WorkingDirectory=`pwd`" >> /lib/systemd/system/lorans.service
echo "#executable is a bash script which calls jar" >> /lib/systemd/system/lorans.service
echo "ExecStart=`pwd`/network-server.sh" >> /lib/systemd/system/lorans.service
echo "SuccessExitStatus=143" >> /lib/systemd/system/lorans.service
echo "TimeoutStopSec=10" >> /lib/systemd/system/lorans.service
echo "Restart=on-failure" >> /lib/systemd/system/lorans.service
echo "RestartSec=5" >> /lib/systemd/system/lorans.service
echo "[Install]" >> /lib/systemd/system/lorans.service
echo "WantedBy=multi-user.target" >> /lib/systemd/system/lorans.service
echo "" >> /lib/systemd/system/lorans.service

systemctl enable lorans.service
echo "Everything done!"
echo "Restarting systemctl"
systemctl daemon-reload
echo "Daemon is located at /lib/systemd/system/lorans.service" 
echo "To disable this service, run this command as super user:" 
echo "   systemctl disable lorans.service"


#!/bin/bash

# Default service name and script location
service_name="lones"
script_location="$(pwd)/network-server.sh"

# Check root user
if [[ $EUID -ne 0 ]]; then
    echo "ERROR: This script must be run as root."
    exit 1
fi

read -p "Enter the service name (default: $service_name): " custom_service_name
read -p "Enter the script location (default: $script_location): " custom_script_location

# Use default values if user input is empty
service_name="${custom_service_name:-$service_name}"
script_location="${custom_script_location:-$script_location}"

echo "Creating launch bash script..."

# Create a bash script that runs the jar file
cat > "$script_location" <<EOF
#!/bin/bash
/usr/bin/java -jar network-server.jar
EOF

chmod 755 "$script_location"

# Install as a system service
service_file="/lib/systemd/system/$service_name.service"

echo "Installing JAR application as a system service..."
cat > "$service_file" <<EOF
[Unit]
Description=LoRa Network Server

[Service]
User=$SUDO_USER
WorkingDirectory=$(pwd)
ExecStart=$script_location
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

systemctl enable "$service_name.service"
echo "Everything done!"
echo "Restarting systemctl"
systemctl daemon-reload
echo "Daemon is located at $service_file"
echo "To disable this service, run this command as super user:"
echo "   systemctl disable $service_name.service"

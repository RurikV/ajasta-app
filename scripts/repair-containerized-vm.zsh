#!/usr/bin/env zsh
# Repair script to install Docker and containerized services on existing VM
# This script fixes VMs that were created without the containerized metadata
# and installs all missing container management tools

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd ssh scp

ajasta_VM_NAME=${ajasta_VM_NAME:-ajasta-host}
DOCKERHUB_USER=${DOCKERHUB_USER:-vladimirryrik}

echo "🔧 Repairing containerized setup on VM '$ajasta_VM_NAME'..."
echo "🏷️ Using DockerHub user: $DOCKERHUB_USER"

# Get VM IP
VM_IP=$(yc compute instance get --name "$ajasta_VM_NAME" --format json |
  jq -r '.network_interfaces[].primary_v4_address.one_to_one_nat.address' |
  grep -E '^[0-9]+(\.[0-9]+){3}$' | head -n1)

if [ -z "$VM_IP" ]; then
  echo "❌ Error: could not resolve public IP for VM '$ajasta_VM_NAME'" >&2
  exit 1
fi

echo "📍 VM IP: $VM_IP"

# Test SSH connectivity
echo "🔍 Testing SSH connectivity..."
if ! ./ssh-ajasta.zsh "echo 'SSH test successful'" >/dev/null 2>&1; then
    echo "❌ Error: Cannot connect to VM via SSH" >&2
    echo "   Try: ./ssh-ajasta.zsh 'echo test'" >&2
    exit 1
fi

echo "✅ SSH connection established"

# Create comprehensive repair script for VM
echo "📝 Creating repair script for VM..."
cat > /tmp/vm-containerized-repair.sh <<'EOF'
#!/bin/bash
set -e

DOCKERHUB_USER="${1:-vladimirryrik}"

echo "🔧 Installing Docker and containerized services..."
echo "🏷️ Using DockerHub user: $DOCKERHUB_USER"

# Check if Docker is already installed
if command -v docker >/dev/null 2>&1; then
    echo "✅ Docker is already installed"
    docker --version
else
    echo "📦 Installing Docker..."
    # Install Docker
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ubuntu
    usermod -aG docker ajasta
    echo "✅ Docker installed successfully"
fi

# Create /opt/ajasta directory
echo "📁 Creating /opt/ajasta directory..."
mkdir -p /opt/ajasta
chown ajasta:ajasta /opt/ajasta

# Create Docker network and volume
echo "🌐 Setting up Docker network and volume..."
docker network create ajasta-net 2>/dev/null || echo "Network ajasta-net already exists"
docker volume create ajasta_pg_data 2>/dev/null || echo "Volume ajasta_pg_data already exists"

echo "📋 Creating container management scripts..."

# Create status.sh
cat > /opt/ajasta/status.sh << 'STATUSEOF'
#!/bin/bash
echo "=== Ajasta Container Status ==="
docker ps --filter "name=ajasta-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "=== Container Logs (last 10 lines each) ==="
for container in ajasta-postgres ajasta-backend ajasta-frontend; do
  if docker ps -q -f name=$container >/dev/null 2>&1; then
    echo "--- $container ---"
    docker logs --tail 10 $container 2>&1
    echo ""
  else
    echo "--- $container ---"
    echo "Error response from daemon: No such container: $container"
    echo ""
  fi
done
STATUSEOF
chmod +x /opt/ajasta/status.sh

# Create start-containers.sh
cat > /opt/ajasta/start-containers.sh << 'STARTEOF'
#!/bin/bash
set -e

# Get DockerHub user from environment or use parameter
DOCKERHUB_USER="${DOCKERHUB_USER:-vladimirryrik}"

echo "Starting Ajasta containerized application..."
echo "Using DockerHub user: $DOCKERHUB_USER"

# Create network and volume if they don't exist
docker network create ajasta-net 2>/dev/null || echo "Network ajasta-net already exists"
docker volume create ajasta_pg_data 2>/dev/null || echo "Volume ajasta_pg_data already exists"

# Stop and remove existing containers
docker stop ajasta-frontend ajasta-backend ajasta-postgres 2>/dev/null || true
docker rm ajasta-frontend ajasta-backend ajasta-postgres 2>/dev/null || true

# Start PostgreSQL
echo "Starting PostgreSQL container..."
docker run -d --name ajasta-postgres \
  --network ajasta-net \
  -p 15432:5432 \
  -e POSTGRES_DB=ajastadb \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=adminpw \
  -v ajasta_pg_data:/var/lib/postgresql/data \
  postgres:16-alpine

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
  if docker exec ajasta-postgres pg_isready -U admin -d ajastadb >/dev/null 2>&1; then
    echo "PostgreSQL is ready!"
    break
  fi
  echo "Waiting for PostgreSQL... ($i/30)"
  sleep 2
done

# Start backend
echo "Starting backend container..."
docker run -d --name ajasta-backend \
  --network ajasta-net \
  -p 8090:8090 \
  -e DB_URL=jdbc:postgresql://ajasta-postgres:5432/ajastadb \
  -e DB_USERNAME=admin \
  -e DB_PASSWORD=adminpw \
  -e JWT_SECRET=change-me-production \
  $DOCKERHUB_USER/ajasta-backend:alpine

# Start frontend on port 80
echo "Starting frontend container on port 80..."
docker run -d --name ajasta-frontend \
  --network ajasta-net \
  -p 80:80 \
  $DOCKERHUB_USER/ajasta-frontend:alpine

# Get external IP and inject it into frontend files
echo "Injecting external IP into frontend configuration..."
EXTERNAL_IP=$(curl -s http://ifconfig.me 2>/dev/null || echo 'VM_IP')
echo "External IP: $EXTERNAL_IP"

# Wait for frontend container to be ready
sleep 5

# Replace ${EXTERNAL_IP} with actual IP in frontend JavaScript files
docker exec ajasta-frontend find /usr/share/nginx/html/static/js -name "*.js" -exec sed -i "s/\${EXTERNAL_IP}/$EXTERNAL_IP/g" {} \; || true
docker exec ajasta-frontend find /usr/share/nginx/html/static/js -name "*.js" -exec sed -i "s/\\\${external_ip}/$EXTERNAL_IP/g" {} \; || true

echo "All containers started!"
echo "Frontend: http://$(curl -s http://ifconfig.me 2>/dev/null || echo 'VM_IP') (port 80)"
echo "Backend: http://$(curl -s http://ifconfig.me 2>/dev/null || echo 'VM_IP'):8090"
echo "PostgreSQL: $(curl -s http://ifconfig.me 2>/dev/null || echo 'VM_IP'):15432"
STARTEOF
chmod +x /opt/ajasta/start-containers.sh

# Create stop-containers.sh
cat > /opt/ajasta/stop-containers.sh << 'STOPEOF'
#!/bin/bash
echo "Stopping Ajasta containers..."
docker stop ajasta-frontend ajasta-backend ajasta-postgres 2>/dev/null || true
echo "Removing stopped containers..."
docker rm ajasta-frontend ajasta-backend ajasta-postgres 2>/dev/null || true
echo "Containers stopped and removed."
STOPEOF
chmod +x /opt/ajasta/stop-containers.sh

# Create systemd service
echo "⚙️ Creating systemd service..."
cat > /etc/systemd/system/ajasta-containers.service << 'SERVICEEOF'
[Unit]
Description=Ajasta Containerized Application
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
Environment="DOCKERHUB_USER=vladimirryrik"
ExecStart=/opt/ajasta/start-containers.sh
ExecStop=/opt/ajasta/stop-containers.sh
TimeoutStartSec=300

[Install]
WantedBy=multi-user.target
SERVICEEOF

systemctl daemon-reload
systemctl enable ajasta-containers

# Set ownership
chown -R ajasta:ajasta /opt/ajasta

echo "✅ Containerized setup repair complete!"
echo ""
echo "🔧 Available commands:"
echo "   Check containers:    /opt/ajasta/status.sh"
echo "   Start containers:    /opt/ajasta/start-containers.sh"
echo "   Stop containers:     /opt/ajasta/stop-containers.sh"
echo "   Restart service:     sudo systemctl restart ajasta-containers"
echo "   Service status:      sudo systemctl status ajasta-containers"
echo ""
echo "🏷️ DockerHub user is set to: $DOCKERHUB_USER"
echo "💡 To change DockerHub user, edit /etc/systemd/system/ajasta-containers.service"
EOF

# Copy and execute repair script on VM
echo "🚀 Executing repair script on VM..."
scp -i ./ajasta_ed25519 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null /tmp/vm-containerized-repair.sh ajasta@$VM_IP:/tmp/

echo "🔧 Running repair process (this may take several minutes)..."
./ssh-ajasta.zsh "sudo bash /tmp/vm-containerized-repair.sh $DOCKERHUB_USER"

echo ""
echo "✅ Repair completed! Testing container management tools..."
sleep 3

echo "🔍 Testing status script..."
./ssh-ajasta.zsh "/opt/ajasta/status.sh"

echo ""
echo "🔍 Testing systemd service..."
./ssh-ajasta.zsh "sudo systemctl status ajasta-containers --no-pager"

echo ""
echo "🎉 Container management tools successfully installed!"
echo ""
echo "📊 Your VM now has:"
echo "   ✅ Docker installed and configured"
echo "   ✅ /opt/ajasta/status.sh - container status checker"
echo "   ✅ /opt/ajasta/start-containers.sh - container starter"
echo "   ✅ /opt/ajasta/stop-containers.sh - container stopper"  
echo "   ✅ ajasta-containers.service - systemd service"
echo ""
echo "🔧 Usage examples:"
echo "   ./ssh-ajasta.zsh '/opt/ajasta/status.sh'"
echo "   ./ssh-ajasta.zsh 'sudo systemctl restart ajasta-containers'"
echo "   ./ssh-ajasta.zsh '/opt/ajasta/start-containers.sh'"
echo ""
echo "🏷️ Using DockerHub user: $DOCKERHUB_USER"
echo "📍 VM IP: $VM_IP"

# Cleanup
rm -f /tmp/vm-containerized-repair.sh
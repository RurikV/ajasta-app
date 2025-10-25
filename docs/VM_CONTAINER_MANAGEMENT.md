# VM Container Management

### 1. Repair Script: `scripts/repair-containerized-vm.zsh` ✅

This script installs all missing container management tools on your existing VM.

**What it does:**
- ✅ Installs Docker if not present
- ✅ Creates `/opt/ajasta` directory structure
- ✅ Installs `/opt/ajasta/status.sh` - container status checker
- ✅ Installs `/opt/ajasta/start-containers.sh` - container starter
- ✅ Installs `/opt/ajasta/stop-containers.sh` - container stopper
- ✅ Creates `ajasta-containers.service` systemd service
- ✅ Sets up Docker network (`ajasta-net`) and volume (`ajasta_pg_data`)
- ✅ Configures proper permissions and ownership

### 2. Test Script: `scripts/test-vm-repair.zsh` ✅

Comprehensive test suite to verify the repair worked correctly.

## Quick Fix Instructions

### Step 1: Run the Repair Script
```bash
# Navigate to scripts directory
cd scripts

# Run repair script (default: vladimirryrik DockerHub user)
./repair-containerized-vm.zsh

# Or with custom DockerHub user
DOCKERHUB_USER=yourusername ./repair-containerized-vm.zsh
```

### Step 2: Verify the Fix
```bash
# Test that everything was installed correctly
./test-vm-repair.zsh

# Or manually test
./ssh-ajasta.zsh "/opt/ajasta/status.sh"
./ssh-ajasta.zsh "sudo systemctl status ajasta-containers"
```

### Step 3: Use Your Container Management Tools
```bash
# Check container status (this was missing before)
./ssh-ajasta.zsh "/opt/ajasta/status.sh"

# Start containers
./ssh-ajasta.zsh "/opt/ajasta/start-containers.sh"

# Restart containers via systemd
./ssh-ajasta.zsh "sudo systemctl restart ajasta-containers"

# Stop containers
./ssh-ajasta.zsh "/opt/ajasta/stop-containers.sh"
```

## Detailed Usage

### Available Commands After Repair

| Command | Description | Example |
|---------|-------------|---------|
| `/opt/ajasta/status.sh` | Show container status and logs | `./ssh-ajasta.zsh "/opt/ajasta/status.sh"` |
| `/opt/ajasta/start-containers.sh` | Start all containers | `./ssh-ajasta.zsh "/opt/ajasta/start-containers.sh"` |
| `/opt/ajasta/stop-containers.sh` | Stop all containers | `./ssh-ajasta.zsh "/opt/ajasta/stop-containers.sh"` |
| `sudo systemctl restart ajasta-containers` | Restart via systemd | `./ssh-ajasta.zsh "sudo systemctl restart ajasta-containers"` |
| `sudo systemctl status ajasta-containers` | Check service status | `./ssh-ajasta.zsh "sudo systemctl status ajasta-containers"` |

### Expected Output After Repair

When you run `/opt/ajasta/status.sh`, you should see:

```bash
=== Ajasta Container Status ===
NAMES             STATUS         PORTS
ajasta-postgres   Up X minutes   0.0.0.0:15432->5432/tcp
ajasta-backend    Up X minutes   0.0.0.0:8090->8090/tcp
ajasta-frontend   Up X minutes   0.0.0.0:80->80/tcp

=== Container Logs (last 10 lines each) ===
--- ajasta-postgres ---
[PostgreSQL startup logs]

--- ajasta-backend ---
[Backend startup logs or "No such container" if image missing]

--- ajasta-frontend ---
[Frontend startup logs or "No such container" if image missing]
```

## Troubleshooting

### Issue: Containers Show "No such container"

**Cause**: Docker images are missing from DockerHub

**Solution**: Build and push images first
```bash
# Build and push images
DOCKERHUB_USER=vladimirryrik ./build-and-push-images.zsh

# Then restart containers
./ssh-ajasta.zsh "sudo systemctl restart ajasta-containers"
```

### Issue: SSH Connection Failed

**Cause**: SSH key not authorized or VM not accessible

**Solution**: 
```bash
# Add SSH key
SSH_USERNAME=ajasta SSH_PUBKEY_FILE=./ajasta_ed25519.pub ./add-ssh-key.zsh ajasta-host

# Test connection
./ssh-ajasta.zsh "echo 'Connection test'"
```

### Issue: Docker Permission Denied

**Cause**: User not in docker group

**Solution**: The repair script automatically adds users to docker group, but you may need to reconnect SSH:
```bash
./ssh-ajasta.zsh "sudo usermod -aG docker ajasta && echo 'User added to docker group'"
```

### Issue: Service Fails to Start

**Cause**: Docker daemon not running or images missing

**Solution**:
```bash
# Check Docker daemon
./ssh-ajasta.zsh "sudo systemctl status docker"

# Check service logs
./ssh-ajasta.zsh "sudo journalctl -u ajasta-containers -n 20"

# Restart Docker if needed
./ssh-ajasta.zsh "sudo systemctl restart docker"
```

## Environment Configuration

### DockerHub User Configuration

The repair script uses `vladimirryrik` as the default DockerHub user. To change:

**Option 1: Environment Variable**
```bash
DOCKERHUB_USER=yourusername ./repair-containerized-vm.zsh
```

**Option 2: Edit Systemd Service (after repair)**
```bash
./ssh-ajasta.zsh "sudo sed -i 's/DOCKERHUB_USER=.*/DOCKERHUB_USER=yourusername/' /etc/systemd/system/ajasta-containers.service"
./ssh-ajasta.zsh "sudo systemctl daemon-reload"
./ssh-ajasta.zsh "sudo systemctl restart ajasta-containers"
```

### Expected Docker Images

For full functionality, these images should exist on DockerHub:
- `vladimirryrik/ajasta-postgres:alpine` (or postgres:16-alpine as fallback)
- `vladimirryrik/ajasta-backend:alpine`
- `vladimirryrik/ajasta-frontend:alpine`

## Testing Your Repair

### Automated Testing
```bash
# Run comprehensive test
./test-vm-repair.zsh
```

### Manual Testing Steps
```bash
# 1. Check SSH access
./ssh-ajasta.zsh "echo 'SSH works'"

# 2. Check Docker installation
./ssh-ajasta.zsh "docker --version"

# 3. Check directory structure
./ssh-ajasta.zsh "ls -la /opt/ajasta/"

# 4. Test status script
./ssh-ajasta.zsh "/opt/ajasta/status.sh"

# 5. Check systemd service
./ssh-ajasta.zsh "sudo systemctl status ajasta-containers"

# 6. Check Docker resources
./ssh-ajasta.zsh "docker network ls | grep ajasta"
./ssh-ajasta.zsh "docker volume ls | grep ajasta"
```

## Before vs After Repair

### Before Repair ❌
```bash
$ ./ssh-ajasta.zsh '/opt/ajasta/status.sh'
bash: line 1: /opt/ajasta/status.sh: No such file or directory

$ ./ssh-ajasta.zsh 'sudo systemctl restart ajasta-containers'
Failed to restart ajasta-containers.service: Unit ajasta-containers.service not found.
```

### After Repair ✅
```bash
$ ./ssh-ajasta.zsh '/opt/ajasta/status.sh'
=== Ajasta Container Status ===
NAMES             STATUS         PORTS
ajasta-postgres   Up 5 minutes   0.0.0.0:15432->5432/tcp

$ ./ssh-ajasta.zsh 'sudo systemctl status ajasta-containers'
● ajasta-containers.service - Ajasta Containerized Application
   Loaded: loaded (/etc/systemd/system/ajasta-containers.service; enabled)
   Active: active (exited) since [timestamp]
```

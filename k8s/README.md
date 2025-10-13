# Kubernetes Infrastructure (Simplified 2-Node Setup)

Ansible playbooks for provisioning a minimal 2-node Kubernetes cluster in Yandex Cloud with simplified networking.

## Overview

This directory contains a streamlined setup for creating a basic Kubernetes cluster:
- **1 master node** with static public IP
- **1 worker node** with static public IP
- Both nodes have direct internet access (no bastion/ProxyJump required)
- Simplified configuration for learning and development purposes

## Infrastructure Architecture

```
┌─────────────────────────────────────────────┐
│         Yandex Cloud (ru-central1-b)        │
│                                             │
│  ┌────────────────────────────────────┐    │
│  │  External Network                  │    │
│  │  (172.16.17.0/28)                  │    │
│  └────────────────────────────────────┘    │
│                                             │
│  ┌────────────────────────────────────┐    │
│  │  Internal Network                  │    │
│  │  (10.10.0.0/24)                    │    │
│  │                                     │    │
│  │  ┌──────────────┐  ┌─────────────┐ │    │
│  │  │  k8s-master  │  │ k8s-worker-1│ │    │
│  │  │              │  │             │ │    │
│  │  │  Static IP:  │  │  Static IP: │ │    │
│  │  │  (public)    │  │  (public)   │ │    │
│  │  │              │  │             │ │    │
│  │  │  4GB RAM     │  │  3GB RAM    │ │    │
│  │  │  2 CPU       │  │  2 CPU      │ │    │
│  │  └──────────────┘  └─────────────┘ │    │
│  │                                     │    │
│  └────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

## Prerequisites

- **Ansible** installed on your local machine
- **Yandex Cloud CLI** (`yc`) installed and configured (`yc init`)
- **jq** installed (for JSON parsing)
- **zsh** available (scripts are written in zsh)
- **SSH key pair** (optional; auto-detected from `~/.ssh/`)

## Directory Structure

```
k8s/
├── README.md                 # This file
├── group_vars/
│   └── all.yml              # Configuration variables
├── inventory.ini.example     # Example inventory template
├── inventory.ini            # Auto-generated inventory (after provisioning)
├── yc-create.yml            # Playbook to create infrastructure
├── yc-destroy.yml           # Playbook to destroy infrastructure
└── generate-inventory.sh    # Script to generate inventory from YC VMs
```

## Configuration Files

### `group_vars/all.yml`

Main configuration file with all customizable parameters:

**Cloud Settings:**
- `yc_cloud_id`: Yandex Cloud ID (auto-detected if empty)
- `yc_folder_id`: Yandex Cloud folder ID (auto-detected if empty)
- `yc_zone`: Deployment zone (default: `ru-central1-b`)

**Network Configuration:**
- `yc_network_name`: External network name
- `yc_subnet_name`: External subnet name
- `yc_internal_network_name`: Internal (private) network name
- `yc_internal_subnet_name`: Internal subnet name

**Static IPs:**
- `master_address_name`: Static IP resource name for master
- `worker1_address_name`: Static IP resource name for worker1

**VM Configuration:**
- `master_vm_name`: Master node name (default: `k8s-master`)
- `worker1_vm_name`: Worker node name (default: `k8s-worker-1`)
- `master_vm_memory`: Master RAM in GB (default: 4)
- `master_vm_cores`: Master CPU cores (default: 2)
- `worker_vm_memory`: Worker RAM in GB (default: 3)
- `worker_vm_cores`: Worker CPU cores (default: 2)

**Bootstrap Options:**
- `bootstrap_k8s`: Enable/disable automatic cluster bootstrap (default: `true`)
- `install_rancher`: Install Rancher dashboard (default: `true`)

### `inventory.ini.example`

Template showing the expected inventory format:

```ini
[local]
localhost ansible_connection=local

[k8s_master]
k8s-master ansible_host=<MASTER_PUBLIC_IP> ansible_user=ajasta ansible_become=true

[k8s_workers]
k8s-worker-1 ansible_host=<WORKER1_PUBLIC_IP> ansible_user=ajasta ansible_become=true

[k8s:children]
k8s_master
k8s_workers

[k8s:vars]
ansible_ssh_private_key_file=~/.ssh/id_rsa
ansible_ssh_common_args=-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null
```

## Quick Start

### 1. Configure Variables

Edit `k8s/group_vars/all.yml` if needed. At minimum, verify:
- `yc_cloud_id` and `yc_folder_id` (auto-detected if you have `yc` CLI configured)
- `ssh_pubkey_file` (auto-detected from `~/.ssh/`)

**Auto-detection behavior:**
- Cloud IDs: Checks environment variables `YC_CLOUD_ID`/`YC_FOLDER_ID`, then `yc` CLI config
- SSH keys: Looks for `~/.ssh/id_ed25519.pub`, `~/.ssh/id_rsa.pub`, or `./scripts/ajasta_ed25519.pub`

### 2. Create Infrastructure

```bash
# From project root
ansible-playbook -i k8s/inventory.ini.example k8s/yc-create.yml
```

This will:
1. Create/reuse external and internal VPC networks
2. Create 2 static public IP addresses
3. Create master VM with static IP
4. Create worker VM with static IP
5. Bootstrap Kubernetes cluster (if `bootstrap_k8s: true`)
6. Install Rancher dashboard (if `install_rancher: true`)
7. Generate `k8s/inventory.ini` with actual IP addresses

### 3. Verify Deployment

After provisioning completes:

```bash
# Check the generated inventory
cat k8s/inventory.ini

# SSH to master node
ssh ajasta@<MASTER_PUBLIC_IP>

# On master, check cluster status
kubectl get nodes -o wide
kubectl get pods -A

# Check Rancher (if installed)
# Access via browser: https://<MASTER_PUBLIC_IP>:30443
# Default password: admin
```

### 4. Generate/Update Inventory

To regenerate the inventory file after manual changes:

```bash
cd k8s
./generate-inventory.sh
```

This script:
- Fetches current VM IPs from Yandex Cloud
- Generates `inventory.ini` with actual addresses
- Both master and worker use direct SSH (no ProxyJump needed)

### 5. Destroy Infrastructure

When you're done and want to save costs:

```bash
# From project root
ansible-playbook -i k8s/inventory.ini.example k8s/yc-destroy.yml
```

This will remove:
- Both VMs (master and worker)
- Both static IP addresses
- Service account and key
- Networks (if not used by other resources)

**Cost saving tip:** Always destroy when not using the cluster to avoid unnecessary charges!

## Usage Examples

### Basic Operations

```bash
# Create infrastructure only (no Kubernetes bootstrap)
# Edit group_vars/all.yml: bootstrap_k8s: false
ansible-playbook -i k8s/inventory.ini.example k8s/yc-create.yml

# Create with Kubernetes but without Rancher
# Edit group_vars/all.yml: bootstrap_k8s: true, install_rancher: false
ansible-playbook -i k8s/inventory.ini.example k8s/yc-create.yml

# Full setup with everything
# Edit group_vars/all.yml: bootstrap_k8s: true, install_rancher: true
ansible-playbook -i k8s/inventory.ini.example k8s/yc-create.yml

# Destroy everything
ansible-playbook -i k8s/inventory.ini.example k8s/yc-destroy.yml
```

### Access Kubernetes

```bash
# SSH to master
ssh ajasta@<MASTER_PUBLIC_IP>

# On master, use kubectl
kubectl get nodes
kubectl get pods --all-namespaces
kubectl cluster-info

# Copy kubeconfig to local machine (optional)
scp ajasta@<MASTER_PUBLIC_IP>:/home/ajasta/.kube/config ./kubeconfig
export KUBECONFIG=./kubeconfig
kubectl get nodes  # Now works from local machine
```

### Access Rancher Dashboard

1. Get master public IP from `k8s/inventory.ini`
2. Open browser: `https://<MASTER_PUBLIC_IP>:30443`
3. Accept self-signed certificate warning
4. Login with default password: `admin`
5. Set new password when prompted

### SSH to Worker Node

Since the worker has a public IP, you can SSH directly:

```bash
# No ProxyJump needed!
ssh ajasta@<WORKER1_PUBLIC_IP>

# Check node status
sudo systemctl status kubelet
sudo crictl ps  # List containers
```

## Configuration Tuning

### VM Resources

Edit `group_vars/all.yml`:

```yaml
# Minimum recommended (may be unstable)
master_vm_memory: 2  # GB
master_vm_cores: 2

# Recommended for stable operation
master_vm_memory: 4  # GB
master_vm_cores: 2

# Worker can be smaller
worker_vm_memory: 3  # GB
worker_vm_cores: 2
```

**Note:** 2GB RAM for master is the absolute minimum and may cause API server failures. 4GB is recommended for stable operation.

### SSH Retry Tuning

Edit `group_vars/all.yml`:

```yaml
ssh_retry_attempts: 20    # Number of retry attempts
ssh_retry_delay: 6        # Seconds between retries
ssh_cmd_timeout: 300      # Max execution time per command (seconds)
```

Increase `ssh_cmd_timeout` if operations need more time (slow network, large downloads).

### Bootstrap Control

```yaml
# Skip Kubernetes bootstrap (only create VMs)
bootstrap_k8s: false

# Bootstrap Kubernetes but skip Rancher
bootstrap_k8s: true
install_rancher: false

# Full setup
bootstrap_k8s: true
install_rancher: true
```

## Troubleshooting

### Cloud ID/Folder ID Not Found

**Problem:** Playbook fails with "yc_cloud_id and yc_folder_id must be provided"

**Solution:**
```bash
# Configure yc CLI
yc init

# Or set environment variables
export YC_CLOUD_ID="your-cloud-id"
export YC_FOLDER_ID="your-folder-id"

# Or set directly in group_vars/all.yml
```

### SSH Key Not Found

**Problem:** VMs created but can't SSH

**Solution:**
```bash
# Add SSH key manually to existing VM
cd scripts
SSH_USERNAME=ajasta SSH_PUBKEY_FILE=~/.ssh/id_rsa.pub ./add-ssh-key.zsh k8s-master
SSH_USERNAME=ajasta SSH_PUBKEY_FILE=~/.ssh/id_rsa.pub ./add-ssh-key.zsh k8s-worker-1
```

### Kubernetes API Server Not Starting

**Problem:** Master node created but API server fails to start

**Common causes:**
- Insufficient RAM (2GB is too tight)
- Slow VM initialization
- Network issues

**Solution:**
```bash
# SSH to master
ssh ajasta@<MASTER_PUBLIC_IP>

# Check kubelet logs
sudo journalctl -u kubelet -f

# Check API server logs
sudo crictl logs <api-server-container-id>

# If RAM is the issue, destroy and recreate with more memory
# Edit group_vars/all.yml: master_vm_memory: 4
ansible-playbook -i k8s/inventory.ini.example k8s/yc-destroy.yml
ansible-playbook -i k8s/inventory.ini.example k8s/yc-create.yml
```

### Worker Not Joining Cluster

**Problem:** Worker VM created but not visible in `kubectl get nodes`

**Solution:**
```bash
# SSH to worker
ssh ajasta@<WORKER1_PUBLIC_IP>

# Check kubelet status
sudo systemctl status kubelet
sudo journalctl -u kubelet -f

# Check if worker can reach master
ping <MASTER_INTERNAL_IP>

# Try rejoining manually
# First get join command from master
ssh ajasta@<MASTER_PUBLIC_IP>
kubeadm token create --print-join-command

# Then on worker, run the join command with sudo
sudo kubeadm join ...
```

### Static IP Already Exists

**Problem:** Playbook fails because static IP already exists

**This is normal!** The playbook is idempotent - it will reuse existing resources. Just continue.

### Inventory Not Generated

**Problem:** `inventory.ini` not created after provisioning

**Solution:**
```bash
# Generate manually
cd k8s
./generate-inventory.sh

# Check if VMs exist
yc compute instance list

# If VMs don't exist, provision failed - check logs
```

## Related Documentation

- Main project README: `../README.md`
- Deployment scripts: `../scripts/README.md`
- VM management: `../VM_CONTAINER_MANAGEMENT.md`

## Notes

- The playbooks call scripts from `../scripts/` directory for actual resource provisioning
- All resources are idempotent - safe to run multiple times
- VM names, network names, and other identifiers can be customized in `group_vars/all.yml`
- Default OS image: CentOS Stream 9 (compatible with RHEL-based systems)
- Bootstrap script also supports Debian/Ubuntu

## License

Part of the Ajasta App project. See main project README for license information.

# Create Kubernetes Cluster on Yandex Cloud

This playbook creates a complete Kubernetes cluster on Yandex Cloud without deploying the Ajasta application.

## What This Playbook Does

### Part 1: Infrastructure Provisioning
- Creates Yandex Cloud VPC networks and subnets (external and internal)
- Provisions static IP addresses for all nodes
- Creates VMs (1 master + 3 workers by default)
- Bootstraps Kubernetes cluster using kubeadm
- Generates inventory.ini with VM IP addresses
- Optionally installs Rancher dashboard

### Part 2: Cluster Configuration
- Installs storage backend (Longhorn or Rook-Ceph)
- Installs Nginx ingress controller
- Configures ingress with external IP
- Verifies storage backend health
- Performs cluster readiness checks

## Prerequisites

### 1. Yandex Cloud CLI
```bash
# Install Yandex Cloud CLI
curl -sSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash

# Initialize and authenticate
yc init
```

### 2. SSH Key Pair
Ensure you have an SSH key pair. The playbook will auto-detect:
- `~/.ssh/id_ed25519.pub`
- `~/.ssh/id_rsa.pub`
- Or specify in `k8s/group_vars/all.yml`: `ssh_pubkey_file`

### 3. Ansible
```bash
# Install Ansible
pip install ansible

# Or on macOS
brew install ansible
```

### 4. Configuration
Edit `k8s/group_vars/all.yml` to configure:
- Yandex Cloud settings (cloud_id, folder_id, zone)
- VM resources (memory, CPU, disk size)
- Network configuration
- Worker node count
- Storage backend preference

## Usage

### Basic Usage (with Rook-Ceph storage)
```bash
ansible-playbook -i k8s/inventory.ini k8s/create-k8s-cluster.yml
```

### With Longhorn Storage Backend
```bash
ansible-playbook -i k8s/inventory.ini k8s/create-k8s-cluster.yml \
  -e "storage_backend=longhorn"
```

### Skip Kubernetes Bootstrap (Infrastructure Only)
```bash
ansible-playbook -i k8s/inventory.ini k8s/create-k8s-cluster.yml \
  -e "bootstrap_k8s=false"
```

### Skip Rancher Dashboard Installation
```bash
ansible-playbook -i k8s/inventory.ini k8s/create-k8s-cluster.yml \
  -e "install_rancher=false"
```

### Custom VM Configuration
```bash
ansible-playbook -i k8s/inventory.ini k8s/create-k8s-cluster.yml \
  -e "worker_vm_memory=8" \
  -e "worker_vm_cores=4" \
  -e "storage_backend=longhorn"
```

## Parameters

| Parameter | Description | Default | Options |
|-----------|-------------|---------|---------|
| `storage_backend` | Storage backend to install | `rook` | `longhorn`, `rook` |
| `bootstrap_k8s` | Bootstrap Kubernetes cluster | `true` | `true`, `false` |
| `install_rancher` | Install Rancher dashboard | `true` | `true`, `false` |
| `worker_vm_memory` | Worker node RAM (GB) | `6` | Integer (GB) |
| `worker_vm_cores` | Worker node CPU cores | `2` | Integer |
| `master_vm_memory` | Master node RAM (GB) | `6` | Integer (GB) |
| `master_vm_cores` | Master node CPU cores | `2` | Integer |

## After Cluster Creation

### 1. Verify Cluster
```bash
# SSH to master node
ssh ajasta@<master-ip>

# Check nodes
kubectl get nodes

# Expected output:
# NAME           STATUS   ROLES           AGE   VERSION
# k8s-master     Ready    control-plane   10m   v1.33.x
# k8s-worker-1   Ready    <none>          9m    v1.33.x
# k8s-worker-2   Ready    <none>          9m    v1.33.x
# k8s-worker-3   Ready    <none>          9m    v1.33.x
```

### 2. Verify Storage Backend
```bash
# Check StorageClass
kubectl get storageclass

# Expected output:
# NAME                 PROVISIONER                     RECLAIMPOLICY   ...
# longhorn (default)   driver.longhorn.io              Delete          ...
# # OR
# longhorn (default)   rook-ceph.rbd.csi.ceph.com      Delete          ...

# For Longhorn
kubectl get pods -n longhorn-system

# For Rook-Ceph
kubectl get pods -n rook-ceph
```

### 3. Verify Ingress Controller
```bash
kubectl get ingressclass

# Expected output:
# NAME    CONTROLLER             PARAMETERS   AGE
# nginx   k8s.io/ingress-nginx   <none>       5m

kubectl get svc -n ingress-nginx

# Expected output shows ingress-nginx-controller with EXTERNAL-IP
```

### 4. Access Rancher Dashboard (if installed)
```bash
# Get Rancher password
kubectl get secret --namespace cattle-system bootstrap-secret \
  -o go-template='{{.data.bootstrapPassword|base64decode}}{{"\n"}}'

# Access in browser
# https://<master-ip>
# Username: admin
# Password: (from command above)
```

### 5. Deploy Applications
```bash
# Deploy Ajasta application
ansible-playbook -i k8s/inventory.ini k8s/deploy-ajasta.yml \
  -e "storage_backend=longhorn"
```

## Differences from Original Playbooks

### `create-k8s-cluster.yml` vs `yc-create.yml`
| Feature | yc-create.yml | create-k8s-cluster.yml |
|---------|---------------|------------------------|
| Infrastructure | ✓ Yes | ✓ Yes |
| K8s Bootstrap | ✓ Yes | ✓ Yes |
| Storage Backend | ✗ No | ✓ Yes |
| Ingress Controller | ✗ No | ✓ Yes |
| Application Deployment | ✗ No | ✗ No |

### `create-k8s-cluster.yml` vs `deploy-ajasta.yml`
| Feature | deploy-ajasta.yml | create-k8s-cluster.yml |
|---------|-------------------|------------------------|
| Infrastructure | ✗ No (requires existing) | ✓ Yes |
| K8s Bootstrap | ✗ No (requires existing) | ✓ Yes |
| Storage Backend | ✓ Yes | ✓ Yes |
| Ingress Controller | ✓ Yes | ✓ Yes |
| Application Deployment | ✓ Yes (PostgreSQL, Backend, Frontend) | ✗ No |

### Summary
- `yc-create.yml`: Creates VMs and K8s cluster only
- `create-k8s-cluster.yml`: Creates complete cluster with storage + ingress (no app)
- `deploy-ajasta.yml`: Deploys Ajasta app to existing cluster

## Typical Workflow

### Option 1: Complete Cluster Creation + App Deployment (Two Steps)
```bash
# Step 1: Create cluster with storage and ingress
ansible-playbook -i k8s/inventory.ini k8s/create-k8s-cluster.yml \
  -e "storage_backend=longhorn"

# Step 2: Deploy Ajasta application
ansible-playbook -i k8s/inventory.ini k8s/deploy-ajasta.yml \
  -e "storage_backend=longhorn"
```

### Option 2: Original Workflow (Separate Steps)
```bash
# Step 1: Create VMs and K8s cluster
ansible-playbook -i k8s/inventory.ini k8s/yc-create.yml

# Step 2: Deploy app with storage and ingress
ansible-playbook -i k8s/inventory.ini k8s/deploy-ajasta.yml \
  -e "storage_backend=longhorn"
```

## Troubleshooting

### Issue: "yc_cloud_id and yc_folder_id must be provided"
**Solution**: Run `yc init` or set in `k8s/group_vars/all.yml`

### Issue: "SSH key not found"
**Solution**: 
```bash
# Generate SSH key
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -N ""

# Or specify in group_vars/all.yml
ssh_pubkey_file: "/path/to/your/key.pub"
```

### Issue: Storage backend pods not running
**Solution**: 
```bash
# For Longhorn - check worker nodes have required packages
ssh ajasta@<worker-ip>
sudo apt install open-iscsi -y
sudo systemctl enable --now iscsid

# For Rook-Ceph - check disk space and resources
kubectl describe nodes
kubectl get pods -n rook-ceph
kubectl logs -n rook-ceph -l app=rook-ceph-operator --tail=100
```

### Issue: Disk pressure on worker nodes
**Solution**:
```bash
# SSH to each worker node
ssh ajasta@<worker-ip>

# Clean up disk space
sudo docker system prune -af
sudo crictl rmi --prune
df -h
```

### Issue: Ingress controller not getting external IP
**Solution**: The playbook patches the ingress-nginx-controller Service with the master node's public IP as externalIPs. Verify:
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller -o yaml
# Should show externalIPs field with master public IP
```

## Configuration Files

### Required Files
- `k8s/group_vars/all.yml` - Global configuration
- `k8s/inventory.ini.j2` - Inventory template (auto-generated)
- `k8s/roles/storage_longhorn/` - Longhorn installation role
- `k8s/roles/storage_rook/` - Rook-Ceph installation role
- `scripts/create-network.zsh` - Network creation script
- `scripts/create-static-ip.zsh` - Static IP creation script
- `scripts/create-vm-static-ip.zsh` - VM creation script
- `scripts/setup-k8s-cluster.zsh` - K8s bootstrap script
- `scripts/install-rancher.zsh` - Rancher installation script

### Generated Files
- `k8s/inventory.ini` - Auto-generated from VMs (backed up automatically)
- `k8s/inventory.ini.backup.<timestamp>` - Backup of previous inventory

## Cost Estimation

Yandex Cloud costs (approximate, ru-central1-b zone):
- Master: 6GB RAM, 2 cores, 30GB disk ≈ $30-40/month
- Worker x3: 6GB RAM, 2 cores, 30GB disk each ≈ $90-120/month
- Static IPs x4: ≈ $5-10/month
- **Total: ~$125-170/month**

To reduce costs:
- Use 4GB RAM for workers: `-e "worker_vm_memory=4"`
- Use 2 workers instead of 3 (edit `k8s/group_vars/all.yml`)
- Stop VMs when not in use (not recommended for production)

## Cleanup

To destroy all resources:
```bash
ansible-playbook -i k8s/inventory.ini k8s/yc-destroy.yml
```

## Support

For issues related to:
- Yandex Cloud: https://cloud.yandex.com/docs/support/
- Kubernetes: https://kubernetes.io/docs/
- Longhorn: https://longhorn.io/docs/
- Rook-Ceph: https://rook.io/docs/
- Nginx Ingress: https://kubernetes.github.io/ingress-nginx/

## License

See project LICENSE file.

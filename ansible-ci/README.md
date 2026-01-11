# Ansible CI - Kubernetes Installation & Application Deployment

This directory contains Ansible playbooks for installing Kubernetes on Terraform-provisioned VMs and deploying the Ajasta application.

## Purpose

`ansible-ci` is designed to work with infrastructure created by Terraform in the `terraform/` directory. Unlike `ansible-k8s` (which provisions VMs and bootstraps K8s), `ansible-ci` focuses on:

1. **Installing Kubernetes** on VMs already created by Terraform
2. **Upgrading Kubernetes** to new versions safely
3. **Deploying applications** using Helm charts
4. **Managing the application lifecycle** (deploy, upgrade, destroy, status)

## Key Features

- **Version-Flexible Installation**: Install any Kubernetes version (1.29.x, 1.30.x, 1.34.x, etc.)
- **Safe Upgrade System**: Automated Kubernetes upgrades with node draining and rollback support
- **Modular Design**: Separate playbooks for installation, worker join, and upgrades
- **Production-Ready**: Package pinning, health checks, and automatic backups

## Prerequisites

### 1. Terraform Infrastructure
- VMs must be created by running `terraform apply` in the `terraform/` directory
- Terraform outputs `terraform/outputs.json` must exist

### 2. SSH Access
- SSH key pair configured (default: `~/.ssh/id_rsa` or `~/.ssh/id_ed25519`)
- SSH access to master VM (public IP from Terraform)
- SSH user: `ajasta` (configured in Terraform)

### 3. Required Tools
```bash
# Install on macOS
brew install ansible jq kubectl helm

# Install on Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y ansible jq python3-pip
pip3 install kubernetes.core helm
```

## Directory Structure

```
ansible-ci/
├── README.md                              # This file
├── k8s-install.yml                        # Initial Kubernetes installation (version-flexible)
├── k8s-join-workers.yml                   # Join worker nodes to cluster
├── k8s-upgrade.yml                        # Upgrade Kubernetes to new version
├── deploy-apps.yml                        # Deploy Ajasta applications
├── destroy-apps.yml                       # Uninstall applications
├── status.yml                             # Check cluster and app status
├── inventory.ini                          # Generated from Terraform outputs
├── group_vars/
│   └── all.yml                            # Global variables
├── roles/
│   ├── containerd_config/                 # Configure containerd CRI
│   ├── cri_ready/                         # Prepare CRI for Kubernetes
│   ├── cni_install/                       # Install Flannel CNI
│   ├── kubeadm_init/                      # Initialize cluster with kubeadm
│   ├── kubernetes_install/                # Install Kubernetes packages (version-flexible)
│   ├── kubernetes_upgrade/                # Upgrade Kubernetes cluster safely
│   ├── ports_verify/                      # Verify required ports
│   ├── registry/                          # Configure container registry
│   └── system_checks/                     # System prerequisite checks
└── scripts/
    ├── install-k8s.sh                     # Interactive installation script
    ├── upgrade-k8s.sh                     # Interactive upgrade script
    ├── update-kubeconfig.sh               # Update local kubeconfig from master
    ├── generate-inventory-from-terraform.sh  # Generate inventory from Terraform outputs (with validation)
    ├── update-and-generate-inventory.sh   # Orchestrated workflow: fetch + verify + generate
    └── generate-inventory-from-yc.sh      # Fallback: generate inventory directly from Yandex Cloud
```

## Quick Start

### ⚠️ Important: GitLab Terraform Backend

If you're using GitLab CI/CD with HTTP backend (which you are), the Terraform state is stored in GitLab, not locally. You need to fetch outputs first.

**Three Methods Available:**

#### Method 1: Orchestrated Workflow (Recommended)

Automatically fetches fresh outputs, validates against actual infrastructure, generates inventory, and tests connectivity:

```bash
cd ansible-ci
export GITLAB_PAT="glpat-your-token-here"
./scripts/update-and-generate-inventory.sh production
```

**What it does:**
1. Fetches fresh Terraform outputs from GitLab
2. Verifies outputs match actual Yandex Cloud VMs
3. Generates Ansible inventory with proper naming
4. Tests SSH connectivity to all nodes
5. Reports any issues with actionable steps

#### Method 2: Terraform-Based (When Outputs Are Fresh)

Generate inventory from existing `terraform/outputs.json`:

```bash
cd ansible-ci
./scripts/generate-inventory-from-terraform.sh
```

**Features:**
- Checks outputs.json age (warns if >60 minutes old)
- Tests SSH connectivity to all nodes
- Validates inventory format
- Clear error messages if issues detected

#### Method 3: Yandex Cloud Fallback (When Terraform Is Broken)

Generate inventory directly from actual running VMs:

```bash
cd ansible-ci
./scripts/generate-inventory-from-yc.sh
```

**Use when:**
- GitLab PAT is unavailable/expired
- Terraform state is stale or corrupted
- Quick recovery during incidents
- You need absolute certainty about actual VMs

**See:** [GITLAB_TERRAFORM_WORKFLOW.md](GITLAB_TERRAFORM_WORKFLOW.md) for detailed GitLab workflow instructions.

### 1. Generate Inventory from Terraform

After fetching outputs, generate the Ansible inventory:

```bash
cd ansible-ci
./scripts/generate-inventory-from-terraform.sh
```

This creates `inventory.ini` with VM IPs from Terraform outputs and validates connectivity.

### 2. Test Inventory Connectivity

```bash
# Test master node
ansible master-node -i inventory.ini -m ping

# Test all nodes
ansible k8s -i inventory.ini -m ping

# Test specific groups
ansible cluster_master -i inventory.ini -m ping  # Master only
ansible cluster_workers -i inventory.ini -m ping  # Workers only
```

**Note:** The inventory uses a new naming convention to avoid Ansible warnings:
- `master-node` (instead of `k8s-master` which conflicted with group name)
- `worker-node-0`, `worker-node-1`, etc.
- Group aliases: `k8s_master`, `k8s_workers`, `k8s` still work

### 3. Install Kubernetes

Deploy Kubernetes on the Terraform-provisioned VMs:

```bash
# Install specific version (e.g., 1.34.3)
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.34.3

# OR use the interactive script
./scripts/install-k8s.sh --version 1.34.3
```

This will:
- Configure containerd on all nodes
- Install Kubernetes packages (kubelet, kubeadm, kubectl) with version pinning
- Initialize the cluster on master
- Install Flannel CNI plugin
- Join worker nodes to cluster

### 4. Update Local Kubeconfig

```bash
./scripts/update-kubeconfig.sh
```

This configures your local `kubectl` to access the cluster.

### 5. Verify Cluster

```bash
kubectl get nodes
kubectl get pods -A
```

### 6. Deploy Applications

After Kubernetes is installed, deploy the Ajasta application:

```bash
ansible-playbook -i inventory.ini deploy-apps.yml
```

This will:
- Create application namespace
- Deploy PostgreSQL database
- Deploy backend API (Spring Boot)
- Deploy frontend (React)
- Configure Ingress for external access
- Create necessary secrets

### 7. Check Status

Verify the deployment:

```bash
ansible-playbook -i inventory.ini status.yml
```

## Configuration

### Global Variables

Edit `group_vars/all.yml` to customize:

```yaml
# SSH settings
ssh_username: "ajasta"
ssh_private_key_file: ""  # Auto-detected if empty

# Kubernetes (default - can be overridden via CLI)
kubernetes_version: "1.29.15"  # Can be any version: 1.29.x, 1.30.x, 1.34.x, etc.
pod_network_cidr: "10.244.0.0/16"

# CNI
cni_plugin: "flannel"  # Changed from calico
flannel_version: "v0.26.2"

# Package management
kubernetes_hold_packages: true  # Prevent auto-upgrades
```

### Environment Variables

Set environment variables for application deployment:

```bash
# Docker registry
export CI_REGISTRY="registry.gitlab.com"
export CI_PROJECT_PATH="vladimirryrik/ajasta-app"
export CI_COMMIT_SHA="latest"

# Application secrets
export POSTGRES_PASSWORD="your-password"
export JWT_SECRET="your-jwt-secret"

# AWS S3 (optional)
export AWS_ACCESS_KEY_ID="your-key"
export AWS_SECRET_ACCESS_KEY="your-secret"
export AWS_S3_BUCKET="your-bucket"

# Stripe (optional)
export STRIPE_PUBLIC_KEY="pk_xxx"
export STRIPE_SECRET_KEY="sk_xxx"

# Ingress
export K8S_INGRESS_HOST="ajasta.local"  # Leave empty for catch-all
```

## Playbooks

### k8s-install.yml

Initial Kubernetes installation with version flexibility.

**Features:**
- Version-flexible installation (any 1.29.x, 1.30.x, 1.34.x, etc.)
- Automatic repository URL construction based on version
- Package pinning to prevent auto-upgrades
- Pre-flight checks and verification

**Parameters:**
- `kubernetes_version` - Kubernetes version to install (default: 1.29.15)

**Examples:**
```bash
# Install default version
ansible-playbook -i inventory.ini k8s-install.yml

# Install specific version
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.34.3

# Use environment variable
export KUBERNETES_VERSION=1.30.0
ansible-playbook -i inventory.ini k8s-install.yml
```

### k8s-join-workers.yml

Join worker nodes to the cluster (run automatically by k8s-install.yml).

**Use when:**
- Workers need to rejoin after reset
- Adding new workers to existing cluster

**Example:**
```bash
ansible-playbook -i inventory.ini k8s-join-workers.yml
```

### k8s-upgrade.yml

Upgrade Kubernetes cluster to a new version safely.

**Features:**
- Automatic cluster state backup
- Node draining before upgrade
- Control plane upgraded first, then workers
- Automatic rollback on failure
- Post-upgrade health checks

**Parameters:**
- `kubernetes_target_version` - Target version to upgrade to

**Examples:**
```bash
# Upgrade to 1.34.3
ansible-playbook -i inventory.ini k8s-upgrade.yml -e kubernetes_target_version=1.34.3

# Use interactive script
./scripts/upgrade-k8s.sh --version 1.34.3
```

### deploy-apps.yml

Deploys Ajasta application using Helm charts.

**Features:**
- Creates namespace and secrets
- Deploys PostgreSQL with Longhorn storage
- Deploys backend and frontend
- Configures Ingress for external access

**Examples:**
```bash
# Deploy with custom namespace
ansible-playbook -i inventory.ini deploy-apps.yml -e app_namespace=ajasta-staging

# Deploy with specific image tag
ansible-playbook -i inventory.ini deploy-apps.yml -e docker_image_tag=v1.0.0
```

### destroy-apps.yml

Removes deployed applications.

**Examples:**
```bash
# Uninstall application (keep namespace)
ansible-playbook -i inventory.ini destroy-apps.yml

# Uninstall application and delete namespace
ansible-playbook -i inventory.ini destroy-apps.yml -e destroy_namespace=true
```

### status.yml

Displays cluster and application status.

**Shows:**
- Cluster nodes
- Namespaces
- Pods, Services, Deployments
- Helm releases
- Ingress configuration
- Persistent volumes

## Workflows

### Complete Deployment Workflow

```bash
# 1. Apply Terraform infrastructure
cd terraform
terraform apply
cd ..

# 2. Generate Ansible inventory
cd ansible-ci
export GITLAB_PAT="glpat-your-token-here"
./scripts/generate-inventory-from-terraform.sh

# 3. Install Kubernetes (version-flexible)
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.34.3

# 4. Update local kubeconfig
./scripts/update-kubeconfig.sh

# 5. Verify cluster
kubectl get nodes
kubectl get pods -A

# 6. Deploy applications
export POSTGRES_PASSWORD="changeme"
export JWT_SECRET="changeme"
ansible-playbook -i inventory.ini deploy-apps.yml

# 7. Check status
ansible-playbook -i inventory.ini status.yml
```

### Kubernetes Upgrade Workflow

```bash
# 1. Check current version
kubectl version --short

# 2. Upgrade to new version (e.g., 1.35.0)
./scripts/upgrade-k8s.sh --version 1.35.0

# 3. Verify upgrade
kubectl get nodes
kubectl get pods -A
```

### Application Update Workflow

```bash
# 1. Build and push new Docker images
cd ajasta-backend
docker build -t registry.gitlab.com/vladimirryrik/ajasta-app/backend:new-tag .
docker push registry.gitlab.com/vladimirryrik/ajasta-app/backend:new-tag

# 2. Deploy new version
cd ../ansible-ci
export CI_COMMIT_SHA="new-tag"
ansible-playbook -i inventory.ini deploy-apps.yml

# 3. Verify rollout
ansible-playbook -i inventory.ini status.yml
```

### Cleanup Workflow

```bash
# 1. Destroy applications
ansible-playbook -i inventory.ini destroy-apps.yml -e destroy_namespace=true

# 2. (Optional) Reset Kubernetes cluster
# Run on all nodes via SSH
sudo kubeadm reset --force

# 3. Destroy Terraform infrastructure
cd terraform
terraform destroy
```

## Troubleshooting

### Installation Fails with 403 Forbidden

**Problem:** Package repository returns 403 Forbidden errors

**Root Cause:** Incorrect Kubernetes version extraction causing wrong repository URL

**Solution:**
```bash
# Verify version format (must be x.y.z)
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.34.3

# Check roles/kubernetes_install/defaults/main.yml for correct regex
```

### Workers Not Joining

**Problem:** Worker nodes not joining cluster

**Solution:**
```bash
# Check if workers can reach master
ansible cluster_workers -i inventory.ini -m shell -a "ping -c 3 {{ hostvars['master-node']['ansible_host'] }}"

# Rejoin workers manually
ansible-playbook -i inventory.ini k8s-join-workers.yml
```

### Inventory Issues

#### Problem: All Nodes Unreachable

**Symptoms:**
```
✗ UNREACHABLE: master-node
✗ UNREACHABLE: worker-node-0
✗ UNREACHABLE: worker-node-1
✗ UNREACHABLE: worker-node-2
```

**Root Cause:** Stale IPs in `terraform/outputs.json`

**Solutions (in order):**

**1. Use Yandex Cloud Fallback (Quickest):**
```bash
cd ansible-ci
./scripts/generate-inventory-from-yc.sh
```
This queries actual running VMs directly from Yandex Cloud.

**2. Refresh Terraform Outputs:**
```bash
cd /Users/rurik/IdeaProjects/petrelevich/ajasta-app
export GITLAB_PAT="glpat-your-token-here"
./scripts/get-terraform-outputs-from-gitlab.sh production

cd ansible-ci
./scripts/generate-inventory-from-terraform.sh
```

**3. Check Actual Infrastructure:**
```bash
# Compare outputs.json with actual VMs
cat terraform/outputs.json | jq '.master_public_ip'
yc compute instance list | grep k8s-master
```

#### Problem: outputs.json is Stale

**Symptoms:**
```
⚠️ outputs.json is 120 minutes old (might be stale)
```

**Solution:**
```bash
# Refresh outputs
cd /Users/rurik/IdeaProjects/petrelevich/ajasta-app
export GITLAB_PAT="glpat-your-token-here"
./scripts/get-terraform-outputs-from-gitlab.sh production
```

#### Problem: GitLab State is Out of Sync

**Symptoms:**
```
✗ Outputs mismatch!
  Actual master IP:    89.169.168.149
  Outputs master IP:   158.160.92.226
```

**Solution:** Run terraform:apply in GitLab CI/CD to update state, OR use YC fallback:
```bash
cd ansible-ci
./scripts/generate-inventory-from-yc.sh
```

#### Problem: "401 Unauthorized" from GitLab

**Root Cause:** Expired or invalid GitLab PAT

**Solution:**
```bash
# Check PAT is set
echo $GITLAB_PAT

# Regenerate PAT at:
# https://otusteam.gitlab.yandexcloud.net/-/user_settings/personal_access_tokens
# Required scope: api

export GITLAB_PAT="glpat-new-token-here"
```

### SSH Connection Issues

**Problem:** `SSH connection refused` or `Permission denied`

**Solution:** Check SSH access:
```bash
# Test SSH manually
ssh -i ~/.ssh/id_rsa ajasta@<master-ip>

# Check if VM is running
yc compute instance list

# Verify correct SSH key
ls -la ~/.ssh/id_rsa*
```

### Kubectl Connection Refused

**Problem:** `The connection to the server <server-ip>:6443 was refused`

**Solution:**
```bash
# Update kubeconfig
./scripts/update-kubeconfig.sh

# Verify cluster is running
ssh -i ~/.ssh/id_rsa ajasta@<master-ip> "sudo kubectl get nodes"
```

### Helm Chart Issues

**Problem:** `Chart not found: helm/ajasta-app`

**Solution:** Ensure Helm chart exists:
```bash
ls -la ../helm/ajasta-app/
```

### Pod Pending Issues

**Problem:** Pods stuck in Pending state

**Solution:** Check node resources and taints:
```bash
kubectl describe nodes
kubectl describe pod <pod-name> -n ajasta
```

## Advanced Usage

### Version-Flexible Kubernetes Installation

The installation system supports any Kubernetes version. Examples:

```bash
# Install Kubernetes 1.29.15 (current stable)
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.29.15

# Install Kubernetes 1.34.3 (latest)
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.34.3

# Install specific patch version
ansible-playbook -i inventory.ini k8s-install.yml -e kubernetes_version=1.30.5

# Use environment variable
export KUBERNETES_VERSION=1.31.2
ansible-playbook -i inventory.ini k8s-install.yml
```

### Kubernetes Cluster Upgrades

Safely upgrade your cluster to a new version:

```bash
# Interactive upgrade with automatic backup
./scripts/upgrade-k8s.sh --version 1.35.0

# Manual upgrade
ansible-playbook -i inventory.ini k8s-upgrade.yml -e kubernetes_target_version=1.35.0
```

The upgrade process:
1. Creates automatic backup of cluster state
2. Drains nodes gracefully
3. Upgrades control plane first (master)
4. Upgrades worker nodes one by one
5. Performs health checks after each step
6. Automatic rollback on failure

### Wrapper Scripts

**install-k8s.sh** - Interactive installation script
```bash
./scripts/install-k8s.sh --version 1.34.3
./scripts/install-k8s.sh --version 1.34.3 --inventory inventory.ini
```

**upgrade-k8s.sh** - Interactive upgrade script
```bash
./scripts/upgrade-k8s.sh --version 1.35.0
# Automatic backup created before upgrade
```

**update-kubeconfig.sh** - Update local kubeconfig
```bash
./scripts/update-kubeconfig.sh
# Fetches admin.conf from master and configures TLS for public IP
```

### Custom Inventory

If you don't want to use the inventory generation script, create `inventory.ini` manually:

```ini
[cluster_master]
master-node ansible_host=89.169.168.149 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s

[cluster_workers]
worker-node-0 ansible_host=89.169.180.229 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s
worker-node-1 ansible_host=89.169.169.24 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s
worker-node-2 ansible_host=89.169.180.211 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s

[k8s_master:children]
cluster_master

[k8s_workers:children]
cluster_workers

[k8s:children]
k8s_master
k8s_workers
```

**Note:** Using the new naming convention prevents Ansible warnings about duplicate group/host names.

### Multi-Environment Deployment

```bash
# Staging environment
export K8S_INGRESS_HOST="staging.ajasta.local"
ansible-playbook -i inventory.ini deploy-apps.yml -e app_namespace=ajasta-staging

# Production environment
export K8S_INGRESS_HOST="ajasta.top"
ansible-playbook -i inventory.ini deploy-apps.yml -e app_namespace=ajasta
```

### Rolling Updates

```bash
# Update backend only
ansible-playbook -i inventory.ini deploy-apps.yml \
  -e docker_image_tag=v2.0.0 \
  --skip-tags frontend
```

## Comparison with ansible-k8s

| Feature | ansible-k8s | ansible-ci (NEW) |
|---------|-------------|-------------------|
| VM Provisioning | ✅ Yes (via scripts) | ❌ No (Terraform only) |
| K8s Bootstrap | ✅ Yes (hardcoded 1.29.0) | ✅ Yes (version-flexible) |
| K8s Upgrades | ❌ No | ✅ Yes (automated & safe) |
| App Deployment | ❌ No | ✅ Yes (Helm) |
| Version Management | Manual | Flexible (any 1.29.x, 1.30.x, 1.34.x) |
| Package Pinning | No | Yes (prevents auto-upgrades) |
| Inventory | Manual/static | Dynamic from Terraform |
| Wrapper Scripts | No | Yes (user-friendly) |
| Use Case | Full IaC workflow | CI/CD + K8s management |

## Best Practices

1. **Always run inventory generation** after Terraform apply
2. **Test SSH connectivity** before installation
3. **Use version-flexible installation** - specify exact Kubernetes version
4. **Keep Kubernetes pinned** - prevents unexpected auto-upgrades
5. **Test upgrades in staging** before production
6. **Create backups** before major upgrades (automatic with upgrade script)
7. **Use wrapper scripts** for easier operations
8. **Store secrets in environment variables**, never in Git
9. **Check status** after deployments
10. **Use version tags** for Docker images in production
11. **Monitor cluster health** - especially control plane pods
12. **Keep Longhorn snapshots** before major changes

### Version Management

- **Pin Kubernetes versions** to avoid unexpected upgrades
- **Test new versions** in non-production environments first
- **Upgrade one minor version at a time** (e.g., 1.29.x → 1.30.x → 1.31.x)
- **Keep backups** before upgrading
- **Monitor after upgrade** - check pod restarts and node status

### Production Checklist

Before deploying to production:
- [ ] Terraform infrastructure applied successfully
- [ ] Inventory generated with correct IPs
- [ ] SSH connectivity verified to all nodes
- [ ] Kubernetes version tested in staging
- [ ] Firewall rules allow cluster communication
- [ ] Sufficient resources on all nodes (CPU, RAM, disk)
- [ ] Backup strategy configured
- [ ] Monitoring and logging in place

## Support

For issues or questions:
1. Check playbook logs: `-vvv` flag for verbose output
2. Review Terraform outputs in `terraform/outputs.json`
3. Check cluster logs: `kubectl logs -n <namespace> <pod>`
4. See detailed documentation:
   - [K8S_INSTALLATION_UPGRADE_GUIDE.md](K8S_INSTALLATION_UPGRADE_GUIDE.md) - Complete installation & upgrade guide
   - [K8S_INSTALLATION_SUCCESS.md](K8S_INSTALLATION_SUCCESS.md) - Installation summary
   - [GITLAB_TERRAFORM_WORKFLOW.md](GITLAB_TERRAFORM_WORKFLOW.md) - GitLab workflow details
   - [INVENTORY_GENERATION_FIX.md](INVENTORY_GENERATION_FIX.md) - Complete inventory generation guide and troubleshooting

## Version History

### v2.1 (January 12, 2026) - Inventory Generation Overhaul
- ✅ Complete rewrite of inventory generation system
- ✅ Added connectivity validation and testing
- ✅ Fixed Ansible naming conflicts (master-node, worker-node-X)
- ✅ Added outputs.json age validation (warns if >60 min old)
- ✅ Created orchestrated workflow script (fetch + verify + generate)
- ✅ Added Yandex Cloud fallback (queries actual VMs directly)
- ✅ Improved error messages with actionable steps
- ✅ Comprehensive troubleshooting documentation

### v2.0 (January 2026) - Complete Rewrite
- ✅ Version-flexible Kubernetes installation (any version)
- ✅ Automated Kubernetes upgrade system
- ✅ Package pinning to prevent auto-upgrades
- ✅ Wrapper scripts for easy operations
- ✅ Changed from Calico to Flannel CNI
- ✅ Improved error handling and recovery
- ✅ Comprehensive documentation

### v1.0 (Previous)
- Hardcoded Kubernetes 1.29.0 installation
- Basic bootstrap with kubeadm
- Calico CNI plugin
- Manual inventory management

## Current Status

**Last tested configuration:**
- Kubernetes: 1.34.3 (all nodes healthy)
- CNI: Flannel v0.26.2
- Container Runtime: containerd 2.2.1
- OS: CentOS Stream 9
- Platform: Yandex Cloud

**Cluster Health:**
- ✅ All control plane pods running (0 restarts)
- ✅ All worker nodes Ready
- ✅ Package pinning enabled
- ✅ Inventory generation system operational
- ✅ All 4 nodes reachable (master + 3 workers)

**Infrastructure:**
- Master: 89.169.168.149
- Worker-0: 89.169.180.229
- Worker-1: 89.169.169.24
- Worker-2: 89.169.180.211
- ✅ Installation and upgrade systems operational

## License

Same as parent project.

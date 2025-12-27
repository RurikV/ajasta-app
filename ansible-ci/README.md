# Ansible CI - Kubernetes Bootstrap & Application Deployment

This directory contains Ansible playbooks for bootstrapping Kubernetes on Terraform-provisioned VMs and deploying the Ajasta application.

## Purpose

`ansible-ci` is designed to work with infrastructure created by Terraform in the `terraform/` directory. Unlike `ansible-k8s` (which provisions VMs and bootstraps K8s), `ansible-ci` focuses on:

1. **Bootstrapping Kubernetes** on VMs already created by Terraform
2. **Deploying applications** using Helm charts
3. **Managing the application lifecycle** (deploy, upgrade, destroy, status)

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
├── README.md                           # This file
├── k8s-bootstrap.yml                   # Bootstrap K8s on Terraform VMs
├── deploy-apps.yml                     # Deploy Ajasta applications
├── destroy-apps.yml                    # Uninstall applications
├── status.yml                          # Check cluster and app status
├── group_vars/
│   └── all.yml                         # Global variables
├── inventory/
│   └── (generated dynamically)         # Ansible inventory from Terraform
├── roles/
│   ├── containerd_config/              # Configure containerd CRI
│   ├── cri_ready/                      # Prepare CRI for Kubernetes
│   ├── cni_install/                    # Install Calico CNI
│   ├── kubeadm_init/                   # Initialize cluster with kubeadm
│   ├── ports_verify/                   # Verify required ports
│   ├── registry/                       # Configure container registry
│   └── system_checks/                  # System prerequisite checks
└── scripts/
    └── generate-inventory-from-terraform.sh  # Generate inventory from Terraform outputs
```

## Quick Start

### ⚠️ Important: GitLab Terraform Backend

If you're using GitLab CI/CD with HTTP backend (which you are), the Terraform state is stored in GitLab, not locally. You need to fetch outputs first:

```bash
# Option 1: Automated (Recommended)
cd ansible-ci
./scripts/generate-inventory-auto.sh

# Option 2: Manual step-by-step
# Step 1: Set GitLab token
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"

# Step 2: Fetch outputs from GitLab
cd ../../scripts
./get-terraform-outputs-from-gitlab.sh production

# Step 3: Generate inventory
cd ../ansible-ci
./scripts/generate-inventory-from-terraform.sh
```

**See:** [GITLAB_TERRAFORM_WORKFLOW.md](GITLAB_TERRAFORM_WORKFLOW.md) for detailed instructions.

### 1. Generate Inventory from Terraform

After fetching outputs, generate the Ansible inventory:

```bash
cd ansible-ci
./scripts/generate-inventory-auto.sh
```

This creates `inventory.ini` with VM IPs from Terraform outputs.

### 2. Test Inventory Connectivity

```bash
# Test master node
ansible k8s-master -i inventory.ini -m ping

# Test all nodes
ansible k8s -i inventory.ini -m ping
```

### 3. Bootstrap Kubernetes Cluster

Deploy Kubernetes on the Terraform-provisioned VMs:

```bash
ansible-playbook -i inventory.ini k8s-bootstrap.yml
```

This will:
- Configure containerd on all nodes
- Install Kubernetes components (kubeadm, kubelet, kubectl)
- Initialize the cluster (on master)
- Join worker nodes
- Install Calico CNI plugin
- Install NGINX Ingress Controller
- Install Longhorn storage (optional)
- Install Rancher dashboard (optional)

### 4. Deploy Applications

After Kubernetes is bootstrapped, deploy the Ajasta application:

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

### 5. Check Status

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

# Kubernetes
kubernetes_version: "1.29.0"
pod_network_cidr: "10.244.0.0/16"

# CNI
cni_plugin: "calico"
calico_version: "v3.28.0"

# Ingress
ingress_controller_enabled: true

# Storage
longhorn_enabled: true

# Rancher
rancher_enabled: false
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

### k8s-bootstrap.yml

Bootstraps Kubernetes cluster on existing VMs.

**Tags:**
- `verify` - Verify ports only
- `containerd` - Configure containerd only
- `cri` - Configure CRI only
- `kubeadm` - Initialize cluster only
- `cni` - Install CNI only

**Examples:**
```bash
# Full bootstrap
ansible-playbook -i inventory.ini k8s-bootstrap.yml

# Bootstrap only CNI (skip other steps)
ansible-playbook -i inventory.ini k8s-bootstrap.yml --tags cni
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
./scripts/generate-inventory-from-terraform.sh

# 3. Bootstrap Kubernetes
ansible-playbook -i inventory.ini k8s-bootstrap.yml

# 4. Deploy applications
export POSTGRES_PASSWORD="changeme"
export JWT_SECRET="changeme"
ansible-playbook -i inventory.ini deploy-apps.yml

# 5. Check status
ansible-playbook -i inventory.ini status.yml
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

# 2. (Optional) Destroy Kubernetes cluster
ansible-playbook -i ansible-k8s/inventory.ini k8s-destroy.yml

# 3. Destroy Terraform infrastructure
cd terraform
terraform destroy
```

## Troubleshooting

### Inventory Issues

**Problem:** `k8s-master group not found in inventory`

**Solution:** Run the inventory generation script:
```bash
./scripts/generate-inventory-from-terraform.sh
```

### SSH Connection Issues

**Problem:** `SSH connection refused`

**Solution:** Check SSH access:
```bash
# Test SSH manually
ssh -i ~/.ssh/id_rsa ajasta@<master-ip>

# Check if VM is running
yc compute instance list
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

### Custom Inventory

If you don't want to use the inventory generation script, create `inventory.ini` manually:

```ini
[k8s-master]
k8s-master ansible_host=51.250.100.218

[k8s-workers]
k8s-worker-0 ansible_host=10.10.0.4
k8s-worker-1 ansible_host=10.10.0.5
k8s-worker-2 ansible_host=10.10.0.6

[k8s:children]
k8s-master
k8s-workers
```

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

| Feature | ansible-k8s | ansible-ci |
|---------|-------------|------------|
| VM Provisioning | ✅ Yes (via scripts) | ❌ No (Terraform only) |
| K8s Bootstrap | ✅ Yes | ✅ Yes |
| App Deployment | ❌ No | ✅ Yes (Helm) |
| Inventory | Manual/static | Dynamic from Terraform |
| Use Case | Full IaC workflow | CI/CD deployment |

## Best Practices

1. **Always run inventory generation** after Terraform apply
2. **Test SSH connectivity** before bootstrapping
3. **Use tags** for partial re-runs (e.g., `--tags cni`)
4. **Store secrets in environment variables**, never in Git
5. **Check status** after deployments
6. **Use version tags** for Docker images in production
7. **Keep backups** of important data (Longhorn snapshots)

## Support

For issues or questions:
1. Check playbook logs: `-vvv` flag for verbose output
2. Review Terraform outputs in `terraform/outputs.json`
3. Check cluster logs: `kubectl logs -n <namespace> <pod>`

## License

Same as parent project.

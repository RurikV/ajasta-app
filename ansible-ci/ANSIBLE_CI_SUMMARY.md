# ansible-ci - Complete Summary

## Overview

`ansible-ci` is a complete Ansible-based automation system for bootstrapping Kubernetes clusters on Terraform-provisioned VMs and deploying containerized applications using Helm charts.

## What Was Created

### Directory Structure
```
ansible-ci/
├── README.md                               # Comprehensive documentation (11KB)
├── ansible.cfg                             # Ansible configuration
├── k8s-bootstrap.yml                       # Kubernetes bootstrap playbook
├── deploy-apps.yml                         # Application deployment playbook
├── destroy-apps.yml                        # Application removal playbook
├── status.yml                              # Cluster & app status checker
├── group_vars/all.yml                      # Global variables (2.5KB)
├── inventory.ini.example                   # Example inventory file
├── roles/                                  # Reusable Ansible roles (from ansible-k8s)
│   ├── cni_install/                        # Calico CNI installation
│   ├── containerd_config/                  # Containerd configuration
│   ├── cri_ready/                          # CRI readiness checks
│   ├── kubeadm_init/                       # Cluster initialization
│   ├── ports_verify/                       # Port verification
│   ├── registry/                           # Container registry config
│   └── system_checks/                      # System prerequisite checks
└── scripts/
    ├── generate-inventory-from-terraform.sh # Dynamic inventory generator
    └── quick-bootstrap.sh                   # One-command bootstrap script
```

## Key Features

### 1. **Terraform Integration**
- Reads VM IPs from `terraform/outputs.json`
- Auto-generates Ansible inventory
- Works seamlessly with Terraform-provisioned infrastructure

### 2. **Kubernetes Bootstrap**
- Installs containerd CRI
- Bootstraps cluster with kubeadm
- Configures Calico CNI networking
- Installs NGINX Ingress Controller
- Installs Longhorn storage (optional)
- Installs Rancher dashboard (optional)

### 3. **Application Deployment**
- Deploys Ajasta application using Helm
- Creates namespaces and secrets
- Deploys PostgreSQL with persistent storage
- Deploys backend API (Spring Boot)
- Deploys frontend (React)
- Configures Ingress for external access

### 4. **Lifecycle Management**
- **Bootstrap**: `k8s-bootstrap.yml`
- **Deploy**: `deploy-apps.yml`
- **Destroy**: `destroy-apps.yml`
- **Status**: `status.yml`

## Quick Start

### Option 1: Automated (Recommended)
```bash
cd ansible-ci
./scripts/quick-bootstrap.sh
```

### Option 2: Step-by-Step
```bash
# 1. Generate inventory from Terraform
./scripts/generate-inventory-from-terraform.sh

# 2. Test connectivity
ansible k8s-master -i inventory.ini -m ping

# 3. Bootstrap Kubernetes
ansible-playbook -i inventory.ini k8s-bootstrap.yml

# 4. Deploy applications
export POSTGRES_PASSWORD="your-password"
export JWT_SECRET="your-jwt-secret"
ansible-playbook -i inventory.ini deploy-apps.yml

# 5. Check status
ansible-playbook -i inventory.ini status.yml
```

## Architecture

### Integration with Terraform

```
┌─────────────────┐
│   Terraform     │
│ (terraform/)    │
│                 │
│ - Creates VMs   │
│ - Outputs IPs   │
└────────┬────────┘
         │ outputs.json
         ▼
┌─────────────────┐
│   ansible-ci    │
│                 │
│ - Reads outputs │
│ - Generates inv │
│ - Bootstraps K8s│
│ - Deploys apps  │
└─────────────────┘
```

### Workflow

```
1. Terraform Apply → VMs Created
                        ↓
2. Generate Inventory → inventory.ini
                        ↓
3. Bootstrap K8s → Cluster Ready
                        ↓
4. Deploy Apps → Application Running
                        ↓
5. Status Check → Verify Deployment
```

## Configuration

### Global Variables (`group_vars/all.yml`)

```yaml
# SSH Configuration
ssh_username: "ajasta"
ssh_private_key_file: ""  # Auto-detected

# Kubernetes
kubernetes_version: "1.29.0"
pod_network_cidr: "10.244.0.0/16"

# CNI Plugin
cni_plugin: "calico"
calico_version: "v3.28.0"

# Ingress Controller
ingress_controller_enabled: true

# Storage
longhorn_enabled: true

# Rancher Dashboard
rancher_enabled: false
```

### Environment Variables

```bash
# Docker Registry
export CI_REGISTRY="registry.gitlab.com"
export CI_PROJECT_PATH="vladimirryrik/ajasta-app"
export CI_COMMIT_SHA="latest"

# Application Secrets
export POSTGRES_PASSWORD="changeme"
export JWT_SECRET="changeme"

# AWS S3 (optional)
export AWS_ACCESS_KEY_ID="your-key"
export AWS_SECRET_ACCESS_KEY="your-secret"
export AWS_S3_BUCKET="your-bucket"

# Stripe (optional)
export STRIPE_PUBLIC_KEY="pk_xxx"
export STRIPE_SECRET_KEY="sk_xxx"
```

## Comparison: ansible-k8s vs ansible-ci

| Aspect | ansible-k8s | ansible-ci |
|--------|-------------|------------|
| **Purpose** | Infrastructure + K8s | K8s Bootstrap + Apps |
| **VM Creation** | ✅ Yes (scripts) | ❌ No (Terraform) |
| **K8s Bootstrap** | ✅ Yes | ✅ Yes |
| **App Deployment** | ❌ No | ✅ Yes (Helm) |
| **Inventory Source** | Manual/Static | Dynamic (Terraform) |
| **Use Case** | Complete IaC | CI/CD Deployment |
| **Complexity** | Higher | Lower |

## Playbooks Detailed Breakdown

### k8s-bootstrap.yml (450+ lines)

**Stages:**
1. Pre-flight checks (YC credentials, Terraform outputs)
2. Configure containerd on all nodes
3. Install Kubernetes components
4. Initialize cluster (master)
5. Join worker nodes
6. Install Calico CNI
7. Install NGINX Ingress Controller
8. Install Longhorn storage
9. Install Rancher dashboard (optional)
10. Post-bootstrap verification

**Tags:**
- `verify` - Verify ports
- `containerd` - Configure containerd
- `cri` - Configure CRI
- `kubeadm` - Initialize cluster
- `cni` - Install Calico

### deploy-apps.yml (250+ lines)

**Deployments:**
1. Namespace creation
2. Secret creation (PostgreSQL, JWT, AWS, Stripe)
3. PostgreSQL database with Longhorn storage
4. Backend API (Spring Boot)
5. Frontend (React)
6. Ingress configuration
7. Health checks and verification

**Features:**
- Multi-environment support (staging/production)
- Configurable image tags
- Secret management
- Persistent storage
- Ingress routing

### destroy-apps.yml (120+ lines)

**Capabilities:**
1. Uninstall Helm releases
2. Optional namespace deletion
3. Display remaining resources
4. Confirmation prompts

### status.yml (180+ lines)

**Displays:**
- Cluster nodes and status
- All namespaces
- Pods, Services, Deployments
- Helm releases
- Ingress configuration
- Persistent volumes

## Roles (From ansible-k8s)

All roles copied from `ansible-k8s/roles/`:

1. **cni_install** - Installs and configures Calico CNI
2. **containerd_config** - Configures containerd as CRI
3. **cri_ready** - Prepares CRI for Kubernetes
4. **kubeadm_init** - Initializes cluster with kubeadm
5. **ports_verify** - Verifies required ports are open
6. **registry** - Configures container registry mirrors
7. **system_checks** - Checks system prerequisites

## Scripts

### generate-inventory-from-terraform.sh (120+ lines)

**Features:**
- Reads `terraform/outputs.json`
- Parses master and worker IPs
- Generates `inventory.ini`
- Validates connectivity
- Color-coded output

**Usage:**
```bash
./scripts/generate-inventory-from-terraform.sh
```

### quick-bootstrap.sh (200+ lines)

**Features:**
- Prerequisites checking
- Automated inventory generation
- Connectivity testing
- Kubernetes bootstrap
- Application deployment
- Status display
- Interactive prompts

**Usage:**
```bash
./scripts/quick-bootstrap.sh
```

## File Sizes

```
README.md                               11KB
k8s-bootstrap.yml                       19KB
deploy-apps.yml                          9KB
destroy-apps.yml                         4KB
status.yml                               7KB
group_vars/all.yml                      2.5KB
ansible.cfg                             2KB
generate-inventory-from-terraform.sh     4KB
quick-bootstrap.sh                       6KB
inventory.ini.example                    1KB

Total: ~65KB of configuration and documentation
```

## Best Practices

1. **Always generate inventory after Terraform apply**
   ```bash
   ./scripts/generate-inventory-from-terraform.sh
   ```

2. **Test connectivity before bootstrap**
   ```bash
   ansible k8s-master -i inventory.ini -m ping
   ```

3. **Use tags for partial re-runs**
   ```bash
   ansible-playbook -i inventory.ini k8s-bootstrap.yml --tags cni
   ```

4. **Store secrets in environment variables**
   ```bash
   export POSTGRES_PASSWORD="secure-password"
   ```

5. **Check status after deployments**
   ```bash
   ansible-playbook -i inventory.ini status.yml
   ```

6. **Use version tags for production**
   ```bash
   export CI_COMMIT_SHA="v1.0.0"
   ansible-playbook -i inventory.ini deploy-apps.yml
   ```

## Troubleshooting

### Common Issues

1. **Inventory not found**
   ```bash
   # Solution: Regenerate inventory
   ./scripts/generate-inventory-from-terraform.sh
   ```

2. **SSH connection failed**
   ```bash
   # Solution: Test SSH manually
   ssh -i ~/.ssh/id_rsa ajasta@<master-ip>
   ```

3. **Pods stuck in Pending**
   ```bash
   # Solution: Check node resources
   kubectl describe nodes
   kubectl describe pod <pod-name> -n ajasta
   ```

4. **Helm chart not found**
   ```bash
   # Solution: Verify chart exists
   ls -la ../helm/ajasta-app/
   ```

## Documentation

### Main Documentation
- `README.md` - Comprehensive guide (400+ lines)

### Supporting Documentation
- `ansible.cfg` - Inline comments explaining each setting
- `inventory.ini.example` - Detailed inventory examples
- All playbooks include inline documentation

## Future Enhancements

Potential improvements for ansible-ci:

1. **Monitoring Stack**
   - Prometheus deployment
   - Grafana dashboards
   - AlertManager configuration

2. **Logging**
   - ELK Stack deployment
   - Loki integration
   - Log aggregation

3. **CI/CD Integration**
   - GitLab CI/CD job templates
   - Automated testing pipelines
   - Blue-green deployments

4. **Backup/Restore**
   - Velero integration
   - Automated backups
   - Disaster recovery procedures

5. **Multi-Cluster**
   - Multi-cluster management
   - Federation support
   - Cluster mesh

## PS

`ansible-ci` provides a complete solution for:
- Bootstrapping Kubernetes on Terraform infrastructure
- Deploying containerized applications with Helm
- Managing application lifecycle
- Automated workflows with scripts

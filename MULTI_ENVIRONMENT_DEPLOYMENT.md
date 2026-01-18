# Multi-Environment Deployment Guide

## Overview

This guide describes the comprehensive multi-environment deployment system for the Ajasta application. The system supports separate **staging** and **production** environments with isolated infrastructure, configurations, and deployment workflows.

## Architecture

### Environment Separation

```
ajasta-app/
├── environments/
│   ├── staging/
│   │   ├── config.yaml              # Staging configuration
│   │   └── helm-values.yaml         # Staging Helm values
│   └── production/
│       ├── config.yaml              # Production configuration
│       └── helm-values.yaml         # Production Helm values
├── ansible/
│   └── k8s/
│       └── group_vars/
│           └── environments/
│               ├── staging.yml      # Staging Ansible variables
│               └── production.yml   # Production Ansible variables
├── terraform/
│   ├── staging.tfvars               # Staging infrastructure
│   └── production.tfvars            # Production infrastructure
└── scripts/
    └── deploy-to-environment.sh    # Master deployment orchestrator
```

### Environment Differences

| Configuration | Staging | Production |
|--------------|---------|------------|
| **Domain** | staging.ajasta.top | ajasta.top |
| **VM Prefix** | ajasta-staging | ajasta-prod |
| **Workers** | 1 | 3 |
| **Backend Replicas** | 1 | 2 |
| **Frontend Replicas** | 1 | 2 |
| **PostgreSQL Instances** | 1 | 2 (HA) |
| **Storage** | 5Gi | 20Gi |
| **Let's Encrypt** | Staging issuer | Production issuer |
| **Resources** | Minimal (256Mi/512Mi) | Full (512Mi/1Gi) |

## Prerequisites

### 1. Yandex Cloud Configuration

```bash
# Install Yandex Cloud CLI
curl https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash

# Initialize and authenticate
yc init

# Verify authentication
yc config get token
```

### 2. Terraform Installation

```bash
# Install Terraform (macOS)
brew install terraform

# Verify installation
terraform version
```

### 3. Ansible Installation

```bash
# Install Ansible (macOS)
brew install ansible

# Verify installation
ansible --version
```

### 4. Required Tools

```bash
# Install jq for JSON parsing
brew install jq

# Install helm
brew install helm
```

## Deployment Workflow

### Step 0: Terraform Infrastructure Creation

This step is executed in GitLab CI/CD pipeline or manually.

#### Staging Infrastructure

```bash
cd terraform

# Initialize Terraform
terraform init

# Plan staging deployment
terraform plan -var-file=staging.tfvars -out=staging.plan

# Apply staging infrastructure
terraform apply -var-file=staging.tfvars
```

#### Production Infrastructure

```bash
cd terraform

# Initialize Terraform
terraform init

# Plan production deployment
terraform plan -var-file=production.tfvars -out=production.plan

# Apply production infrastructure
terraform apply -var-file=production.tfvars
```

**Resources Created:**
- 4 Static IP addresses (master + 3 workers)
- 2 VPC networks (external + internal)
- 2 VPC subnets
- 4 Compute instances (master + 3 workers)

### Step 1: Generate Ansible Inventory

After Terraform completes, generate Ansible inventory from running VMs:

```bash
cd ansible/k8s

# Generate inventory for staging (filters by VM name prefix)
./generate-inventory-from-yc.sh
```

This script:
- Queries Yandex Cloud for running VMs
- Filters VMs by environment prefix (ajasta-staging or ajasta-prod)
- Creates `inventory.ini` with master and worker IPs
- Tests SSH connectivity

### Step 2: Setup Kubernetes Cluster

Deploy Kubernetes components to the cluster:

```bash
cd ansible/k8s

# Run cluster setup (installs k8s, CNI, Helm, Ingress, Longhorn)
./k8s-cluster-setup.sh
```

**Components Installed:**
1. Prepare nodes (disable firewall, configure sysctl)
2. Install containerd and nerdctl
3. Initialize Kubernetes control plane
4. Join worker nodes to cluster
5. Install Cilium CNI
6. Install Helm package manager
7. Deploy Ingress NGINX Controller
8. Deploy Longhorn Storage

### Step 3: Deploy CloudNativePG and PostgreSQL

Deploy the PostgreSQL operator and create a database cluster:

```bash
cd ansible/k8s

# Deploy CloudNativePG operator and PostgreSQL cluster
ansible-playbook -i inventory.ini 19-deploy-cloudnativepg.yml
```

**Creates:**
- CloudNativePG operator (namespace: cnpg-system)
- PostgreSQL cluster (namespace: postgresql-cluster)
- 1 instance (staging) or 2 instances (production)
- Longhorn storage for persistent volumes
- Secrets and service accounts

### Step 4: Deploy Ajasta Application

Deploy the complete Ajasta application stack:

```bash
cd ansible/ajasta-app

# Deploy all components
./deploy-ajasta.sh
```

**Components Deployed:**
1. PostgreSQL Cluster (CloudNativePG)
2. Backend API (Spring Boot)
3. Frontend (React/Nginx)
4. Ingress Configuration
5. Deployment Verification
6. TLS with Let's Encrypt

### Step 5: Fix Connection Timeout Issues

Apply connection timeout fixes:

```bash
cd ansible/k8s

# Run connection timeout fix playbook
ansible-playbook -i inventory.ini 31-fix-connection-timeout-complete.yml
```

## Quick Deployment

### Deploy to Staging

```bash
cd /path/to/ajasta-app

# Deploy everything to staging
./scripts/deploy-to-environment.sh staging
```

### Deploy to Production

```bash
cd /path/to/ajasta-app

# Deploy everything to production
./scripts/deploy-to-environment.sh production
```

### Deploy with Verbose Output

```bash
# Deploy to staging with verbose Ansible output
./scripts/deploy-to-environment.sh staging -vv

# Deploy to production with very verbose output
./scripts/deploy-to-environment.sh production -vvv
```

### Resume from Specific Step

```bash
# Skip steps 1-2, start from step 3 (CloudNativePG deployment)
./scripts/deploy-to-environment.sh staging --step 3

# Start from step 4 (application deployment)
./scripts/deploy-to-environment.sh production --step 4
```

## Environment Configuration

### Staging Configuration

Location: `environments/staging/config.yaml`

Key settings:
- Domain: `staging.ajasta.top`
- Namespace: `ajasta-staging`
- Minimal resources (1 worker, 1 replica each)
- Let's Encrypt staging issuer
- No backups, no autoscaling

### Production Configuration

Location: `environments/production/config.yaml`

Key settings:
- Domain: `ajasta.top`
- Namespace: `ajasta`
- Full resources (3 workers, 2 replicas each)
- Let's Encrypt production issuer
- Backups enabled, autoscaling enabled

## GitLab CI/CD Integration

### Pipeline Stages

```yaml
stages:
  - terraform:plan
  - terraform:apply
  - k8s:deploy
  - verify
```

### Staging Deployment Job

```yaml
deploy:staging:
  stage: k8s:deploy
  environment:
    name: staging
    url: https://staging.ajasta.top
  script:
    - ./scripts/deploy-to-environment.sh staging -vv
  only:
    - main
  when: manual
```

### Production Deployment Job

```yaml
deploy:production:
  stage: k8s:deploy
  environment:
    name: production
    url: https://ajasta.top
  script:
    - ./scripts/deploy-to-environment.sh production -vv
  only:
    - main
  when: manual
```

## Verification

### Check Cluster Status

```bash
# SSH to master node
ssh -i ~/.ssh/id_rsa_k8s ajasta@<MASTER_IP>

# Check nodes
sudo kubectl get nodes

# Check all pods
sudo kubectl get pods -A

# Check Ajasta pods
sudo kubectl get pods -n ajasta-staging   # for staging
sudo kubectl get pods -n ajasta           # for production

# Check PostgreSQL
sudo kubectl get pods -n postgresql-cluster
```

### Check Application

```bash
# Get ingress URL
kubectl get ingress -n ajasta-staging

# Test frontend
curl -I https://staging.ajasta.top

# Test backend API
curl https://staging.ajasta.topapi/actuator/health
```

## Troubleshooting

### Inventory Generation Fails

**Problem:** `generate-inventory-from-yc.sh` can't find VMs

**Solution:**
1. Verify Terraform apply completed successfully
2. Check VMs are running: `yc compute instance list`
3. Verify VM naming: `yc compute instance list | grep ajasta-staging` or `ajasta-prod`
4. Check YC authentication: `yc config get token`

### Cluster Setup Fails

**Problem:** `k8s-cluster-setup.sh` fails on step 3 (init master)

**Solution:**
1. Check inventory.ini exists and has correct IPs
2. Test SSH: `ansible -i inventory.ini k8s_master -m ping`
3. Review logs in `/var/log/cloud-init-output.log` on master
4. Check firewall rules in Yandex Cloud console

### PostgreSQL Deployment Fails

**Problem:** CloudNativePG deployment fails

**Solution:**
1. Check Longhorn is installed: `kubectl get pods -n longhorn-system`
2. Verify storage class exists: `kubectl get storageclass`
3. Check CloudNativePG operator: `kubectl get pods -n cnpg-system`
4. Review logs: `kubectl logs -n postgresql-cluster -l cnpg.io/podRole=instance`

### Application Not Accessible

**Problem:** Can't access application via domain

**Solution:**
1. Check ingress: `kubectl get ingress -n ajasta-staging`
2. Verify DNS points to ingress IP
3. Check ingress controller: `kubectl get pods -n ingress-nginx`
4. Review certificate: `kubectl get certificate -n ajasta-staging`
5. Check certificate status: `kubectl describe certificate -n ajasta-staging`

## Best Practices

### 1. Always Deploy to Staging First

```bash
# Test in staging
./scripts/deploy-to-environment.sh staging

# Verify everything works

# Then deploy to production
./scripts/deploy-to-environment.sh production
```

### 2. Use Verbose Mode for Debugging

```bash
# Use -vv or -vvv to see detailed Ansible output
./scripts/deploy-to-environment.sh staging -vv
```

### 3. Check Logs After Deployment

```bash
# Check backend logs
kubectl logs -n ajasta-staging -l component=backend -f

# Check frontend logs
kubectl logs -n ajasta-staging -l component=frontend -f
```

### 4. Monitor Resources

```bash
# Check node resource usage
kubectl top nodes

# Check pod resource usage
kubectl top pods -n ajasta-staging
```

### 5. Verify DNS Configuration

Staging: `staging.ajasta.top` → Ingress IP
Production: `ajasta.top` → Ingress IP

Use `dig` or `nslookup` to verify DNS:

```bash
dig staging.ajasta.top
dig ajasta.top
```

## Security Considerations

### 1. Secrets Management

**Never commit secrets to git!**

Use environment variables for secrets:
- `JWT_SECRET`
- `MAIL_USERNAME` / `MAIL_PASSWORD`
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`
- `STRIPE_PUBLIC_KEY` / `STRIPE_SECRET_KEY`

### 2. Production Deployments

Production deployments require explicit confirmation:

```bash
./scripts/deploy-to-environment.sh production
# Type 'production' to confirm
```

### 3. TLS Certificates

- Staging uses Let's Encrypt staging (invalid certificates)
- Production uses Let's Encrypt production (valid certificates)

### 4. Access Control

Kubernetes clusters are protected with:
- SSH key authentication
- Firewall rules in Yandex Cloud
- Network policies (Cilium)
- RBAC (Kubernetes)

## Rollback Procedures

### Quick Rollback

```bash
# Rollback application deployment
cd ansible/ajasta-app

# Redeploy previous version
./deploy-ajasta.sh
```

### Full Rollback

```bash
# 1. Restore previous infrastructure state
cd terraform
terraform apply -var-file=production.tfvars

# 2. Redeploy application
./scripts/deploy-to-environment.sh production --step 4
```

## Maintenance

### Update Application

```bash
# Pull latest code
git pull origin main

# Redeploy to staging
./scripts/deploy-to-environment.sh staging --step 4

# Verify, then deploy to production
./scripts/deploy-to-environment.sh production --step 4
```

### Update Kubernetes Cluster

```bash
# Run cluster setup again (updates components)
cd ansible/k8s
./k8s-cluster-setup.sh 7  # Start from optional components
```

### Scale Resources

Edit environment config files and redeploy:

```bash
# Edit production configuration
vim environments/production/config.yaml

# Redeploy
./scripts/deploy-to-environment.sh production --step 4
```

## Support and Documentation

- Main README: `README.md`
- Terraform docs: `terraform/README.md`
- Ansible docs: `ansible/k8s/README.md`
- Helm docs: `helm/KUBERNETES_HELM_SETUP.md`

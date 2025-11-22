# Terraform GitLab CI/CD Integration

This document explains how to use the Terraform GitLab CI/CD integration for deploying the Ajasta app infrastructure.

## Overview

The Terraform GitLab CI/CD integration provides automated infrastructure deployment for the Kubernetes cluster using Terraform. This replaces the manual infrastructure setup with a fully automated pipeline that creates the necessary VMs, networks, and configurations for running the Ajasta app.

## Architecture

### Infrastructure Components

The Terraform configuration creates:

1. **Virtual Network Infrastructure**
   - External VPC network (`external-ajasta-network`)
   - Internal VPC network (`internal-ajasta-network`)
   - Subnets with proper CIDR allocation
   - Static public IP addresses for all nodes

2. **Kubernetes Cluster Nodes**
   - 1 Master node (`k8s-master`) - 2 vCPU, 6 GB RAM, 30 GB disk
   - 3 Worker nodes (`k8s-worker-1/2/3`) - 2 vCPU, 6 GB RAM, 30 GB disk each
   - Preemptible instances with 20% core fraction for cost optimization
   - CentOS Stream 9 with OS Login support

3. **Security and Access**
   - SSH key injection for secure access
   - Cloud-init user-data for initial configuration
   - Security groups for network isolation

### GitLab CI/CD Pipeline Structure

```
Pipeline Stages:
├── validate
│   ├── validate:syntax          # YAML/Shell validation
│   ├── validate:docker          # Dockerfile linting
│   ├── helm:lint               # Helm chart validation
│   ├── k8s:auth:check          # K8s connectivity check
│   └── terraform:validate      # Terraform syntax/format check
├── plan
│   └── terraform:plan          # Generate Terraform execution plan
├── build
│   ├── build:backend           # Build and push backend image
│   └── build:frontend          # Build and push frontend image
├── test
│   ├── test:backend            # Backend tests
│   └── test:frontend           # Frontend tests
├── package
│   └── package:compose         # Package deployment artifacts
└── deploy
    ├── terraform:apply:dev     # Deploy infra to dev (auto)
    ├── deploy:staging          # Deploy app to staging
    ├── deploy:k8s:staging      # Deploy K8s app to staging
    ├── deploy:tf:production    # DEPLOY INFRA TO PROD (MANUAL)
    ├── deploy:production       # Deploy app to production
    ├── deploy:k8s:production   # Deploy K8s app to production
    └── terraform:destroy:production # Destroy prod infra (manual)
```

## Required GitLab CI/CD Variables

The Terraform CI/CD integration uses your **existing GitLab CI variables**. No additional variables are required!

### Variables Already Used

The pipeline automatically uses these existing variables from your GitLab project:

```bash
# Yandex Cloud Authentication (already configured)
YC_CLOUD_ID              # Your Yandex Cloud ID
YC_FOLDER_ID             # Your Yandex Cloud Folder ID
YC_TOKEN                 # Yandex Cloud IAM token

# SSH Access (already configured)
YC_SSH_PRIVATE_KEY       # SSH private key (public key extracted automatically)
```

### How It Works

1. **Yandex Cloud Authentication**: The pipeline maps:
   - `YC_CLOUD_ID` → `TF_VAR_yc_cloud_id`
   - `YC_FOLDER_ID` → `TF_VAR_yc_folder_id`
   - `YC_TOKEN` → `TF_VAR_yc_token`

2. **SSH Key Management**:
   - Public key is automatically extracted from `YC_SSH_PRIVATE_KEY`
   - No need to manage separate SSH public key variable
   - Uses standard OpenSSH key format

### Verify Your Current Variables

To ensure your existing variables are properly configured:

```bash
# Check current Yandex Cloud configuration
yc config get cloud-id
yc config get folder-id

# Test Yandex Cloud token
yc iam create-token

# Verify SSH key format
ssh-keygen -y -f ~/.ssh/id_rsa  # Should output public key
```

## Usage Instructions

### 1. Development Deployment (Automatic)

For the `develop` branch, infrastructure is deployed automatically:

```bash
# Push to develop branch
git push origin develop

# This triggers:
# - terraform:validate
# - terraform:plan
# - terraform:apply:dev (automatic)
```

### 2. Production Deployment (Manual)

For the `main` branch, infrastructure deployment requires manual approval:

```bash
# 1. Push to main branch
git push origin main

# 2. Go to GitLab CI/CD > Pipelines
# 3. Find the latest pipeline for main branch
# 4. Click on "deploy:tf:production" job
# 5. Review the plan output
# 6. Click "Run" to execute deployment
```

### 3. Monitoring the Deployment

The `deploy:tf:production` job provides:

- **Real-time logs** of Terraform execution
- **Cluster information** with IP addresses and node details
- **Connection details** for SSH access
- **Artifacts** including:
  - `terraform_outputs.env` - Environment variables with cluster IPs
  - `ansible_inventory.ini` - Generated Ansible inventory
  - `fetch_kubeconfig.sh` - Script to download kubeconfig
  - `plan.txt` - Full execution plan

### 4. Application Deployment

After infrastructure is ready, deploy the Ajasta app:

#### Option A: Using GitLab CI/CD
```bash
# After Terraform deployment succeeds, run:
deploy:k8s:production  # Deploy application to Kubernetes
```

#### Option B: Manual Deployment
```bash
# 1. Copy generated inventory
cp terraform/ansible_inventory.ini k8s/inventory.ini

# 2. Deploy the application
ansible-playbook k8s/deploy-ajasta.yml -i k8s/inventory.ini
```

## Terraform State Management

The pipeline uses GitLab's built-in Terraform state backend:

- **State Location**: GitLab Project > Settings > Repository > Terraform state
- **State Name**: `production`
- **State Locking**: Automatic with HTTP backend
- **State Security**: Encrypted and access-controlled

### Managing State

1. **View Current State**:
   ```bash
   # In terraform:state:cleanup job or locally
   terraform state list
   terraform show
   ```

2. **Import Existing Resources**:
   ```bash
   terraform import yandex_compute_instance.master <instance_id>
   ```

3. **Remove Resources from State**:
   ```bash
   terraform state rm yandex_compute_instance.workers["k8s-worker-1"]
   ```

## Troubleshooting

### Common Issues

1. **Authentication Failures**:
   ```bash
   # Check Yandex Cloud token
   yc config get token

   # Verify folder access
   yc resource-manager folder get <folder-id>
   ```

2. **SSH Key Issues**:
   ```bash
   # Test SSH connection
   ssh -i ~/.ssh/id_ed25519 ajasta@<master-ip>

   # Verify key format
   ssh-keygen -l -f ~/.ssh/id_ed25519.pub
   ```

3. **Terraform State Issues**:
   ```bash
   # Force unlock if state is locked
   terraform force-unlock LOCK_ID

   # Refresh state if drift detected
   terraform refresh
   ```

### Debug Mode

Enable verbose logging by setting these variables in GitLab:

```bash
TF_LOG=DEBUG          # Enable Terraform debug logging
TF_LOG_PATH=./tf.log  # Save logs to file
```

### Recovery Procedures

1. **Partial Deployment Recovery**:
   ```bash
   # Check what failed
   terraform plan

   # Apply remaining changes
   terraform apply
   ```

2. **Complete Infrastructure Reset**:
   ```bash
   # WARNING: This destroys everything!
   terraform:destroy:production  # Manual trigger required
   ```

## Cost Optimization

The Terraform configuration includes cost optimization features:

1. **Preemptible Instances**: 80% cost reduction with automatic recovery
2. **Core Fraction**: 20% CPU allocation for lower cost
3. **Network HDD**: Cheaper storage option
4. **Single Zone Deployment**: Reduced network transfer costs

## Security Considerations

1. **IAM Tokens**: Your existing `YC_TOKEN` is used automatically, ensure it's refreshed regularly
2. **SSH Keys**: The pipeline automatically extracts public key from `YC_SSH_PRIVATE_KEY`, no additional setup needed
3. **Network Security**: Uses private networks with NAT gateways for security
4. **State Security**: Terraform state encrypted in GitLab's HTTP backend
5. **Access Control**: Manual production deployment prevents accidental infrastructure changes
6. **Variable Protection**: All CI variables should be marked as "protected" and "masked" in GitLab settings

## Best Practices

1. **Branch Strategy**: Use `develop` for testing, `main` for production
2. **Manual Approvals**: Always review Terraform plans before applying
3. **Monitoring**: Set up alerts for cluster health
4. **Backups**: Regular backups of application data
5. **Documentation**: Keep configuration and processes documented

## Integration with Existing Workflows

The Terraform CI/CD integration is designed to work alongside existing deployment methods:

- **Compatible with**: Current VM deployment via scripts
- **Alternative to**: Manual infrastructure setup
- **Enhancement for**: Kubernetes-based deployments
- **Not affecting**: Docker Compose local development

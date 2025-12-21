# Terraform Environment Setup Guide

## Assignment Summary

This guide describes the completed Terraform assignment #2: setting up multiple environments (staging/production) with remote backend for infrastructure state management.

### What Was Accomplished:

1. **Remote Backend Configured** - HTTP backend integrated with GitLab CI/CD
2. **Environment Configurations Created**:
   - `staging.tfvars` - staging environment
   - `production.tfvars` - production environment
   - `workspaces.tf` - Terraform workspaces configuration
3. **Documentation Updated** - README.md with complete architecture description
4. **Management Script Added** - `manage-workspaces.sh` for easy environment handling
5. **State Example Prepared** - `terraform.tfstate.example` demonstrating state structure

## Quick Start for Testing

### 1. Initialization
```bash
cd terraform
./manage-workspaces.sh init
```

### 2. Create Staging Environment
```bash
./manage-workspaces.sh create staging
./manage-workspaces.sh plan staging
```

### 3. Create Production Environment
```bash
./manage-workspaces.sh create production
./manage-workspaces.sh plan production
```

## Environment-to-Code Binding

Each environment is bound to code through:
- **Terraform Workspaces** - state isolation
- **Variable Files** - separate .tfvars files for each environment
- **Resource Prefixes** - unique resource names
- **Remote Backend** - separate state files in GitLab

## State File Structure

- Staging: `gitlab-state-staging-{pipeline_id}`
- Production: `gitlab-state-production-{pipeline_id}`
- Default: `gitlab-state-default-{pipeline_id}`

## Environment Architecture

### Staging
- Minimal resources (1 master + 1 worker)
- Preemptible VMs for cost savings
- 1 application replica

### Production
- High-availability configuration (1 master + 3 workers)
- Stable non-preemptible VMs
- 3 application replicas
- SSD disks for performance

### Remote Backend Benefits
- Centralized state storage
- Automatic locking
- Environment isolation
- Change history
- CI/CD integration

All configuration files are located in the repository in the `terraform/` directory.

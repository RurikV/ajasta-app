# GitLab CI/CD Terraform Multi-Environment Setup Guide

This guide explains how to set up and use Terraform with GitLab CI/CD for multi-environment infrastructure deployment.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [GitLab CI/CD Variables Configuration](#gitlab-cicd-variables-configuration)
5. [Pipeline Structure](#pipeline-structure)
6. [Usage](#usage)
7. [Environment-Specific Configuration](#environment-specific-configuration)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

---

## Overview

This project uses GitLab CI/CD to deploy Terraform infrastructure to two environments:

- **Staging**: Automatically deployed from the `develop` branch
- **Production**: Manually deployed from the `main` branch

Each environment has its own:
- Terraform state file stored in GitLab
- Configuration variables (tfvars files)
- Kubernetes cluster resources
- Network CIDR ranges

---

## Architecture

### State Management

```
GitLab Project
├── Terraform State (HTTP Backend)
│   ├── staging        (develop branch)
│   └── production     (main branch)
```

### Deployment Flow

```
develop branch ──► Manual Deploy ──► Staging Environment
                                         │
                                         ▼
                                    Manual Promote
                                         │
                                         ▼
main branch ──► Manual Deploy ──► Production Environment
```

---

## Prerequisites

### Required Tools

- **GitLab Runner** with shell executor or Docker executor
- **Terraform** >= 1.5.0
- **Yandex Cloud CLI** (for local testing)
- **jq** (JSON processor)

### GitLab Settings

1. **Project Permissions**:
   - Maintainer role or higher
   - Access to Infrastructure → Terraform

2. **GitLab Runner**:
   - Configure a runner with shell or Docker executor
   - Ensure runner has internet access for provider downloads

---

## GitLab CI/CD Variables Configuration

Navigate to: **Project → Settings → CI/CD → Variables**

### Required Variables

| Variable Name | Type | Protected | Masked | Description |
|---------------|------|-----------|--------|-------------|
| `YC_CLOUD_ID` | Variable | Yes | Yes | Yandex Cloud ID |
| `YC_FOLDER_ID` | Variable | Yes | Yes | Yandex Cloud Folder ID |
| `YC_TOKEN` | Variable | Yes | Yes | Yandex Cloud OAuth token |
| `YC_SSH_PRIVATE_KEY` | Variable | Yes | Yes | SSH private key for VM access |
| `YC_VM_EXTERNAL_IP` | Variable | No | No | (Optional) Static external IP for ingress |

### Optional Variables

| Variable Name | Type | Description |
|---------------|------|-------------|
| `TF_VAR_ssh_username` | Variable | SSH username (default: `ajasta`) |
| `TF_LOG` | Variable | Terraform log level (DEBUG, INFO, WARN) |

---

## Pipeline Structure

### Stages

```yaml
stages:
  - validate    # Format, validate, security scan
  - plan        # Create execution plans
  - apply       # Deploy infrastructure
  - destroy     # Cleanup resources
```

### Jobs

#### Validation Stage

1. **terraform:fmt**
   - Checks Terraform code formatting
   - Runs on: merge requests, develop, main

2. **terraform:validate**
   - Validates Terraform configuration
   - Runs on: merge requests, develop, main

3. **terraform:security-scan**
   - Runs tfsec security scanner
   - Runs on: merge requests, develop, main
   - Allow failure: true

#### Plan Stage

4. **terraform:plan:staging**
   - Creates plan for staging environment
   - Branch: `develop`
   - Automatic execution

5. **terraform:plan:production**
   - Creates plan for production environment
   - Branch: `main`
   - **Manual execution**

#### Apply Stage

6. **terraform:apply:staging**
   - Applies staging infrastructure changes
   - Branch: `develop`
   - **Manual execution** (requires manual approval)

7. **terraform:apply:production**
   - Applies production infrastructure changes
   - Branch: `main`
   - **Manual execution** (requires manual approval)

#### Destroy Stage

8. **terraform:destroy:staging**
   - Destroys staging infrastructure
   - Branch: `develop`
   - **Manual execution**

9. **terraform:destroy:production**
   - Destroys production infrastructure
   - Branch: `main`
   - **Manual execution**

---

## Usage

### Deploy to Staging (Automatic)

1. Create a new branch from `develop`:
   ```bash
   git checkout develop
   git checkout -b feature/my-changes
   ```

2. Make your Terraform changes

3. Commit and push:
   ```bash
   git add .
   git commit -m "feat: add new infrastructure"
   git push origin feature/my-changes
   ```

4. Create a merge request to `develop`

5. Pipeline runs:
   - ✅ Validate → ✅ Plan:Staging → ✅ Apply:Staging

6. Staging environment is automatically deployed

### Deploy to Production (Manual)

1. Merge `develop` to `main`:
   ```bash
   git checkout main
   git merge develop
   git push origin main
   ```

2. Pipeline runs validation automatically

3. **Manual Steps**:
   - Go to **CI/CD → Pipelines**
   - Click on the latest pipeline
   - Find `terraform:plan:production` job
   - Click the **play** button (▶) to run

4. Review the plan output

5. **Manual Steps**:
   - Find `terraform:apply:production` job
   - Click the **play** button (▶) to deploy

6. Production environment is deployed

### Destroy Infrastructure

#### Destroy Staging

1. Go to **CI/CD → Pipelines**
2. Find a pipeline from the `develop` branch
3. Click on `terraform:destroy:staging` job
4. Click the **play** button (▶)

#### Destroy Production

1. Go to **CI/CD → Pipelines**
2. Find a pipeline from the `main` branch
3. Click on `terraform:destroy:production` job
4. Click the **play** button (▶)
5. **Warning**: This will destroy production infrastructure!

---

## Environment-Specific Configuration

### Staging Configuration

File: `terraform/staging.tfvars`

```hcl
# Staging Environment Variables
environment = "staging"
prefix = "ajasta-staging"

yc_cloud_id = "your-staging-cloud-id"
yc_folder_id = "your-staging-folder-id"

# Resource settings (minimal for cost savings)
master_memory = 4
worker_memory = 4
master_cores = 2
worker_cores = 2
worker_count = 1

preemptible = true  # 80% cost savings
boot_disk_size = 20
```

### Production Configuration

File: `terraform/production.tfvars`

```hcl
# Production Environment Variables
environment = "production"
prefix = "ajasta-prod"

yc_cloud_id = "your-production-cloud-id"
yc_folder_id = "your-production-folder-id"

# Resource settings (high availability)
master_memory = 8
worker_memory = 8
master_cores = 4
worker_cores = 4
worker_count = 3

preemptible = false  # Stability
boot_disk_size = 50
```

---

## Troubleshooting

### State Lock Issues

If a job fails with a state lock error:

1. Go to **Infrastructure → Terraform** in GitLab
2. Find the locked state (staging or production)
3. Click **Force unlock** if needed

### Backend Authentication Errors

If you see "HTTP remote state endpoint requires auth":

1. Verify `TF_HTTP_USERNAME` is set to `gitlab-ci-token`
2. Verify `TF_HTTP_PASSWORD` is set to `${CI_JOB_TOKEN}`
3. Check GitLab CI/CD variables configuration

### Variable Not Set Errors

If Terraform can't find variables:

1. Check that `staging.tfvars` or `production.tfvars` exist
2. Verify GitLab CI/CD variables are properly set
3. Check variable names match exactly (case-sensitive)

### Yandex Cloud Provider Errors

For authentication issues with Yandex Cloud:

1. Verify `YC_TOKEN` is valid and not expired
2. Check `YC_CLOUD_ID` and `YC_FOLDER_ID` are correct
3. Ensure the token has necessary permissions

### Job Timeout

If jobs timeout:

1. Check **Settings → CI/CD → Timeout**
2. Increase timeout for `terraform:apply:production` job
3. Verify network connectivity from runner to Yandex Cloud API

---

## Best Practices

### 1. Branch Protection

Protect the `main` branch:
- **Settings → Repository → Protected branches**
- Require merge requests
- Require approval from Maintainer

### 2. Required Approvals

Enable manual approval for production:
- **Settings → CI/CD → Protected branches**
- Set `terraform:apply:production` to manual execution

### 3. State Backup

GitLab automatically backs up Terraform state:
- View at **Infrastructure → Terraform**
- States are versioned

### 4. Variable Security

- **Always mask** sensitive variables (tokens, keys)
- **Protect** production variables
- Never commit secrets to repository

### 5. Cost Management

- Use preemptible instances in staging
- Run `terraform:destroy:staging` when not in use
- Set up scheduled cleanup pipelines

### 6. Monitoring

- Enable GitLab environment monitoring
- Use Terraform outputs to track resource IPs
- Set up external monitoring for deployed services

### 7. Testing

- Always test in staging first
- Review plan output before applying
- Use `terraform:validate` in merge requests

---

## Pipeline Visualization

### Staging Pipeline (develop branch)

```
┌─────────────┐
│   fmt       │
└──────┬──────┘
       ▼
┌─────────────┐
│  validate   │
└──────┬──────┘
       ▼
┌─────────────┐
│security-scan│
└──────┬──────┘
       ▼
┌─────────────┐
│plan:staging │
└──────┬──────┘
       ▼
┌─────────────┐
│apply:staging│  ← Automatic
└─────────────┘
```

### Production Pipeline (main branch)

```
┌─────────────┐
│   fmt       │
└──────┬──────┘
       ▼
┌─────────────┐
│  validate   │
└──────┬──────┘
       ▼
┌─────────────┐
│security-scan│
└──────┬──────┘
       ▼
┌───────────────────┐
│plan:production    │  ← Manual
└─────────┬─────────┘
          ▼
┌───────────────────┐
│apply:production   │  ← Manual
└───────────────────┘
```

---

## Local Development

### Testing Terraform Locally

```bash
# Navigate to terraform directory
cd terraform

# Initialize (no backend)
terraform init -backend=false

# Select workspace (optional)
terraform workspace new staging
# or
terraform workspace new production

# Plan with variables
terraform plan -var-file="staging.tfvars" -out=plan.tfplan

# Apply
terraform apply plan.tfplan
```

---

## Additional Resources

- [GitLab Terraform Documentation](https://docs.gitlab.com/ee/user/infrastructure/iac/terraform_state.html)
- [Terraform GitLab Provider](https://registry.terraform.io/providers/gitlabhq/gitlab/latest/docs)
- [Yandex Cloud Terraform Provider](https://terraform-provider.yandexcloud.net/
- [Terraform Best Practices](https://www.terraform.io/docs/cloud/guides/recommended-practices/index.html)

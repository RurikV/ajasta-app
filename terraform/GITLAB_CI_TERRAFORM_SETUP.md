# GitLab CI/CD Terraform Multi-Environment Setup Guide

This guide explains how to set up and use Terraform with GitLab CI/CD for multi-environment infrastructure deployment to Yandex Cloud.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [GitLab CI/CD Variables Configuration](#gitlab-cicd-variables-configuration)
5. [Pipeline Structure](#pipeline-structure)
6. [Usage](#usage)
7. [Environment-Specific Configuration](#environment-specific-configuration)
8. [Authentication](#authentication)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Overview

This project uses GitLab CI/CD to deploy Terraform infrastructure to two environments:

- **Staging**: Deployed from the `develop` branch (manual approval required)
- **Production**: Deployed from the `main` branch (manual approval required)

Each environment has its own:
- Terraform state file stored in GitLab HTTP backend
- Configuration variables (tfvars files)
- Kubernetes cluster resources (1 master + 3 workers)
- Network infrastructure (VPC, subnets, addresses)

---

## Architecture

### State Management

```
GitLab Project
├── Terraform State (HTTP Backend)
│   ├── staging        (develop branch)
│   └── production     (main branch)
```

### Authentication Flow

```
Plan Job (terraform:plan:staging)
  ↓
Uses: GITLAB_PAT + GITLAB_USERNAME
  ↓
Creates state lock with GITLAB_PAT
  ↓
Plan succeeds
  ↓
Lock released (with HTTP 409 - non-critical)

Apply Job (terraform:apply:staging)
  ↓
Uses: GITLAB_PAT + GITLAB_USERNAME (SAME TOKEN!)
  ↓
Acquires state lock with GITLAB_PAT
  ↓
Creates 12 resources (networks, addresses, VMs)
  ↓
Apply succeeds
  ↓
Lock released (with HTTP 409 - non-critical)
```

**KEY:** Both plan and apply jobs use the **same GITLAB_PAT** for consistent authentication.

### Deployment Flow

```
develop branch ──► terraform:plan:staging (auto)
                 │
                 ▼
         terraform:apply:staging (manual approval)
                 │
                 ▼
         Staging Environment (4 VMs created)
                 │
                 ▼
         Manual Promote to main
                 │
                 ▼
main branch ──► terraform:plan:production (manual)
                 │
                 ▼
         terraform:apply:production (manual approval)
                 │
                 ▼
         Production Environment (4 VMs created)
```

---

## Prerequisites

### Required Tools

- **GitLab Runner** with shell executor
- **Terraform** >= 1.13.0
- **Yandex Cloud CLI** (for local verification)

### GitLab Settings

1. **Project Permissions**:
   - Maintainer role or higher
   - Access to Infrastructure → Terraform state

2. **GitLab Runner**:
   - Configure a runner with shell executor
   - Ensure runner has internet access

---

## GitLab CI/CD Variables Configuration

Navigate to: **Project → Settings → CI/CD → Variables**

### CRITICAL: Required Variables for State Management

| Variable Name | Type | Protected | Masked | Expanded | Description |
|---------------|------|-----------|--------|----------|-------------|
| `GITLAB_PAT` | Variable | **No** | **Yes** | **Yes** | Personal Access Token for state backend |
| `GITLAB_USERNAME` | Variable | **No** | No | **Yes** | Your GitLab username (e.g., Vladimir.Rurik) |

**IMPORTANT:**
- Both variables **MUST be set** for Terraform to work
- Both **MUST NOT be protected** (uncheck "Protected")
- Both **MUST be expanded** (check "Expand variable")

### Required Variables for Yandex Cloud

| Variable Name | Type | Protected | Masked | Expanded | Description |
|---------------|------|-----------|--------|----------|-------------|
| `YC_CLOUD_ID` | Variable | No | Yes | Yes | Yandex Cloud ID |
| `YC_FOLDER_ID` | Variable | No | Yes | Yes | Yandex Cloud Folder ID |
| `YC_TOKEN` | Variable | No | Yes | Yes | Yandex Cloud OAuth token |

### Optional Variables

| Variable Name | Type | Description |
|---------------|------|-------------|
| `TF_VAR_ssh_username` | Variable | SSH username (default: `ajasta`) |
| `YC_SSH_PRIVATE_KEY` | File (Variable) | SSH private key content for VM access |

---

## How to Create GitLab Personal Access Token

### Step 1: Navigate to Token Settings

Go to:
```
https://otusteam.gitlab.yandexcloud.net/-/profile/personal_access_tokens
```

Or:
1. Click your avatar (top-right)
2. **Edit profile**
3. **Access Tokens** (left sidebar)

### Step 2: Create New Token

1. Click **Add new token**
2. Configure:
   - **Token name**: `terraform-state-backend`
   - **Expiration date**: Choose a future date (recommended: 6-12 months)
   - **Select scopes**: ✅ **`api`** (REQUIRED!)
3. Click **Create personal access token**

### Step 3: Copy Token

**IMPORTANT:** Copy the token **IMMEDIATELY** - it will only be shown once!

```
glpat-xxxxxxxxxxxxxxxxxxxxxx
```

Save this token - you'll need it for the next step.

---

## How to Find Your GitLab Username

### Method 1: Check Your Profile URL

Look at your project URL:
```
https://otusteam.gitlab.yandexcloud.net/Vladimir.Rurik/ajasta-app
                                      ^^^^^^^^^^^^
                                      This is your username!
```

### Method 2: Check Your Profile

1. Click your avatar (top-right)
2. Your username is displayed at the top
3. Or go to: `https://otusteam.gitlab.yandexcloud.net/Vladimir.Rurik`

**Your username is likely:** `Vladimir.Rurik`

---

## Adding Variables to GitLab CI/CD

### Step 1: Navigate to Variables

Go to your project:
```
https://otusteam.gitlab.yandexcloud.net/Vladimir.Rurik/ajasta-app
```

Then:
1. **Settings** (left sidebar)
2. **CI/CD**
3. **Variables** (expand section)
4. Click **Add variable**

### Step 2: Add GITLAB_PAT Variable

Configure:
- **Key**: `GITLAB_PAT`
- **Value**: `glpat-xxxxxxxxxxxxxxxxxxxxxx` (paste your token)
- **Type**: Variable
- **Masked**: ✅ Checked (hides token in logs)
- **Protected**: ❌ **UNCHECKED** (critical - must be unchecked for develop branch)
- **Expand variable**: ✅ Checked (required for variable expansion)

Click **Add variable**

### Step 3: Add GITLAB_USERNAME Variable

Configure:
- **Key**: `GITLAB_USERNAME`
- **Value**: `Vladimir.Rurik` (your GitLab username)
- **Type**: Variable
- **Masked**: ❌ Unchecked (username is not secret)
- **Protected**: ❌ **UNCHECKED**
- **Expand variable**: ✅ Checked

Click **Add variable**

### Step 4: Verify Variables

You should see:
```
GITLAB_PAT        ••••••••••••••••••••  Masked  Expanded  All (default)
GITLAB_USERNAME   Vladimir.Rurik        -        Expanded  All (default)
```

---

## Pipeline Structure

### Stages

```yaml
stages:
  - validate    # Terraform fmt and validate
  - plan        # Create execution plans
  - build       # Docker images (not shown)
  - test        # Tests (not shown)
  - package     # Package artifacts (not shown)
  - deploy      # Apply Terraform changes
```

### Terraform Jobs

#### Validation Jobs

**terraform:fmt:**
- Runs: On `develop` and `main` branches
- Purpose: Check Terraform formatting
- Command: `terraform fmt -check -recursive`

**terraform:validate:**
- Runs: On `develop` and `main` branches
- Purpose: Validate Terraform configuration
- Command: `terraform validate`

#### Plan Jobs

**terraform:plan:staging:**
- Runs: On `develop` and `main` branches
- State: `staging`
- Auth: Uses `GITLAB_PAT` + `GITLAB_USERNAME`
- Output: `plan.tfplan` artifact

**terraform:plan:production:**
- Runs: On `main` branch (manual)
- State: `production`
- Auth: Uses `GITLAB_PAT` + `GITLAB_USERNAME`
- Output: `plan.tfplan` artifact

#### Apply Jobs

**terraform:apply:staging:**
- Runs: After plan completes
- Trigger: Manual approval
- State: `staging`
- Auth: Uses `GITLAB_PAT` + `GITLAB_USERNAME` (same as plan!)
- Result: Creates 12 resources

**terraform:apply:production:**
- Runs: After plan completes
- Trigger: Manual approval
- State: `production`
- Auth: Uses `GITLAB_PAT` + `GITLAB_USERNAME`
- Result: Creates 12 resources

---

## Usage

### Deploying to Staging

#### Method 1: Automatic Pipeline (Recommended)

1. Push to `develop` branch:
   ```bash
   git checkout develop
   git merge main
   git push origin develop
   ```

2. Pipeline starts automatically

3. **terraform:plan:staging** runs:
   - Creates plan for staging environment
   - Shows 12 resources to add

4. **terraform:apply:staging** waits:
   - Click the **Play** button (manual approval)
   - 12 resources created in Yandex Cloud

#### Method 2: Manual Trigger

1. Go to: **CI/CD → Pipelines**
2. Click **Run pipeline**
3. Select branch: `develop`
4. Click **Run pipeline**

### Deploying to Production

1. Go to: **CI/CD → Pipelines**
2. Click **Run pipeline**
3. Select branch: `main`
4. Click **Run pipeline**

---

## Environment-Specific Configuration

### Staging Configuration

**File:** `terraform/staging.tfvars`

```hcl
environment = "staging"
prefix      = "ajasta-staging"
yc_zone     = "ru-central1-b"

# VM Configuration
master_core_fraction = 20
worker_core_fraction = 20
master_vm_memory     = 6
master_vm_cores      = 2
worker_vm_cores      = 2
worker_count         = 1  # Reduced for staging

# Resource Settings
preemptible    = true
boot_disk_size = 20
boot_disk_type = "network-hdd"
```

### Production Configuration

**File:** `terraform/production.tfvars`

```hcl
environment = "production"
prefix      = "ajasta-prod"
yc_zone     = "ru-central1-b"

# VM Configuration
master_core_fraction = 50
worker_core_fraction = 50
master_vm_memory     = 8
master_vm_cores      = 4
worker_vm_cores      = 4
worker_count         = 3  # Full cluster

# Resource Settings
preemptible    = false
boot_disk_size = 50
boot_disk_type = "network-ssd"
```

---

## Authentication

### How GitLab HTTP Backend Authentication Works

GitLab's Terraform HTTP backend uses **Basic Authentication** with:

1. **Username**: Your GitLab username (e.g., `Vladimir.Rurik`)
2. **Password**: Your Personal Access Token (e.g., `glpat-xxxx`)

### Required Environment Variables

For GitLab HTTP backend, Terraform requires:

```bash
TF_HTTP_USERNAME="Vladimir.Rurik"      # Your GitLab username
TF_HTTP_PASSWORD="glpat-xxxx"          # Your Personal Access Token
TF_PASSWORD="glpat-xxxx"               # Same as TF_HTTP_PASSWORD (GitLab requirement!)
```

### Why Both TF_HTTP_PASSWORD and TF_PASSWORD?

**GitLab requires BOTH variables to be set:**

1. **TF_HTTP_PASSWORD**: Used by Terraform HTTP backend for API calls
2. **TF_PASSWORD**: Used by GitLab Terraform helpers/integration

From GitLab troubleshooting documentation:
> "If you have set TF_HTTP_PASSWORD, make sure to set the same value as TF_PASSWORD"

### Why Same Token in Plan and Apply Jobs?

**GitLab state locks are TOKEN-SPECIFIC:**

- Lock created with Token A → Can only be accessed with Token A
- Lock created with CI_JOB_TOKEN → Cannot be accessed with GITLAB_PAT
- Different tokens = Authentication failures!

**Solution:** Use **GITLAB_PAT in BOTH plan AND apply jobs** for consistent authentication.

---

## Resources Created

### Total Resources: 12

#### Networks (4)
- `yandex_vpc_network.external` - External network
- `yandex_vpc_network.internal` - Internal network
- `yandex_vpc_subnet.external` - External subnet (172.16.17.0/28)
- `yandex_vpc_subnet.internal` - Internal subnet (10.10.0.0/24)

#### Addresses (4)
- `yandex_vpc_address.master` - Static IP for k8s-master
- `yandex_vpc_address.workers["k8s-worker-1"]` - Static IP for worker-1
- `yandex_vpc_address.workers["k8s-worker-2"]` - Static IP for worker-2
- `yandex_vpc_address.workers["k8s-worker-3"]` - Static IP for worker-3

#### Compute Instances (4)
- `yandex_compute_instance.master` - Kubernetes master node
- `yandex_compute_instance.workers["k8s-worker-1"]` - Kubernetes worker-1
- `yandex_compute_instance.workers["k8s-worker-2"]` - Kubernetes worker-2
- `yandex_compute_instance.workers["k8s-worker-3"]` - Kubernetes worker-3

---

## Troubleshooting

### "Error acquiring the state lock - HTTP remote state endpoint requires auth"

**Cause:** State is locked by another job with different authentication

**Solution 1:** Wait for lock to expire (5-10 minutes)

**Solution 2:** Force unlock via API:
```bash
curl -X DELETE \
  -H "PRIVATE-TOKEN: glpat-o9enasy8qUsiKZQDBQWP" \
  https://otusteam.gitlab.yandexcloud.net/api/v4/projects/1305/terraform/state/staging/lock
```

**Solution 3:** Use quick-unlock script:
```bash
cd terraform
./scripts/quick-unlock.sh glpat-o9enasy8qUsiKZQDBQWP staging
```

### "HTTP remote state endpoint requires auth"

**Cause:** GITLAB_PAT or GITLAB_USERNAME not set correctly

**Solution:**
1. Verify both variables exist in **Settings → CI/CD → Variables**
2. Check GITLAB_USERNAME is your actual GitLab username
3. Check GITLAB_PAT is not expired
4. Ensure both are **Expanded** and **NOT Protected**

### "⚠️ Using CI_JOB_TOKEN (apply job will fail)"

**Cause:** GITLAB_PAT not set

**Solution:** Create GITLAB_PAT variable (see instructions above)

### "Illegal argument zone_id"

**Cause:** yc_zone not defined in tfvars

**Solution:** Fixed in latest commits - yc_zone now defined in both tfvars files

### "Error releasing the state lock - HTTP 409"

**Cause:** Non-critical lock release error (ignorable)

**Impact:** NONE - resources already created successfully

**Solution:** Fixed in latest commits - error is now ignored

---

## Best Practices

### 1. Token Management

**Create tokens with expiration dates:**
- Recommended: 6-12 months
- Set calendar reminder to renew
- Don't use tokens without expiration

**Token rotation:**
- Create new token annually
- Update GITLAB_PAT variable
- Delete old token

### 2. Variable Security

**Mask sensitive variables:**
- GITLAB_PAT: ✅ Masked
- YC_TOKEN: ✅ Masked
- YC_CLOUD_ID: ✅ Masked
- YC_FOLDER_ID: ✅ Masked

**Unprotect variables for develop branch:**
- All variables: ❌ Protected unchecked (for develop branch access)

**Expand all variables:**
- All variables: ✅ Expanded (required for variable expansion)

### 3. State Management

**Never force unlock unless certain:**
- Verify no one else is running Terraform
- Check lock creation time
- Use API unlock when possible

**State lock best practices:**
- Use same token in plan and apply jobs (we do this!)
- Don't interrupt terraform processes
- Let locks expire naturally if possible

### 4. Deployment Workflow

**Test in staging first:**
1. Deploy to staging (develop branch)
2. Verify all resources created
3. Test application deployment
4. Then deploy to production (main branch)

**Manual approvals:**
- Keep manual approval for production
- Consider automatic deployment for staging
- Review plans before applying

### 5. Monitoring

**After deployment:**
```bash
# Check VM status
yc compute instance list

# Check network resources
yc vpc network list

# Check state locks
curl -H "PRIVATE-TOKEN: glpat-xxxx" \
  https://otusteam.gitlab.yandexcloud.net/api/v4/projects/1305/terraform/state/staging
```

---

## Summary

**Required Variables (2):**
1. `GITLAB_PAT` - Personal Access Token with `api` scope
2. `GITLAB_USERNAME` - Your GitLab username

**Yandex Cloud Variables (3):**
1. `YC_CLOUD_ID`
2. `YC_FOLDER_ID`
3. `YC_TOKEN`

**Key Points:**
- ✅ Both GITLAB_PAT and GITLAB_USERNAME MUST be set
- ✅ Both MUST be expanded and NOT protected
- ✅ Same token used in plan and apply jobs
- ✅ Creates 12 resources (4 VMs + infrastructure)
- ✅ Takes ~2 minutes for VM creation

**Next Steps:**
1. Create GitLab Personal Access Token
2. Add GITLAB_PAT and GITLAB_USERNAME to CI/CD variables
3. Push to develop branch
4. Run pipeline
5. Approve apply job
6. VMs created! 


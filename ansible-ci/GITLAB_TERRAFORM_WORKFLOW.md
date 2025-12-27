# GitLab Terraform Workflow Guide

## Problem

When Terraform is run via GitLab CI/CD with the HTTP backend, the Terraform state is stored in GitLab, not locally. This means `outputs.json` doesn't exist on your local machine, preventing Ansible from generating inventory.

## Solution

This guide shows you how to fetch Terraform outputs from GitLab and generate Ansible inventory locally.

## Architecture

```
┌─────────────────┐
│ GitLab CI/CD    │
│ (terraform:apply)│
│                 │
│ ┌─────────────┐ │
│ │ HTTP Backend│ │
│ └──────┬──────┘ │
└────────┼────────┘
         │ State stored in GitLab
         ▼
┌─────────────────┐
│ Local Machine   │
│                 │
│ Fetch outputs   │
│ from GitLab API │
│                 │
│ Generate        │
│ Ansible inv     │
└─────────────────┘
```

## Quick Start

### Option 1: Automated (Recommended)

```bash
cd ansible-ci
./scripts/generate-inventory-auto.sh
```

This script will:
1. Check if `outputs.json` exists locally
2. If not, automatically fetch from GitLab
3. Generate Ansible inventory

### Option 2: Manual, Step-by-Step

#### Step 1: Set up GitLab credentials

```bash
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"
```

**Where to get the token:**
- Go to: https://gitlab.com/-/user_settings/personal_access_tokens
- Create a token with `api` scope
- Required scope: **api** (full read/write access)

#### Step 2: Fetch Terraform outputs from GitLab

```bash
cd scripts
./get-terraform-outputs-from-gitlab.sh production
```

Or for staging:
```bash
./get-terraform-outputs-from-gitlab.sh staging
```

This will:
1. Fetch Terraform state from GitLab HTTP backend
2. Extract outputs from the state
3. Save to `terraform/outputs.json`

#### Step 3: Generate Ansible inventory

```bash
cd ../ansible-ci
./scripts/generate-inventory-from-terraform.sh
```

#### Step 4: Test connectivity

```bash
ansible k8s-master -i inventory.ini -m ping
ansible k8s -i inventory.ini -m ping
```

## Prerequisites

### Required Tools

```bash
# macOS
brew install jq curl ansible

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y jq curl ansible python3-pip
pip3 install kubernetes.core helm
```

### Required Environment Variables

```bash
# GitLab Personal Access Token (required)
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"

# Optional: Auto-detected from git remote
export PROJECT_ID="your-project-id"

# Optional: For local terraform backend
export YC_CLOUD_ID="your-cloud-id"
export YC_FOLDER_ID="your-folder-id"
```

## Scripts Overview

### 1. `get-terraform-outputs-from-gitlab.sh`

**Location:** `scripts/get-terraform-outputs-from-gitlab.sh`

**Purpose:** Fetches Terraform outputs from GitLab HTTP backend

**Usage:**
```bash
./get-terraform-outputs-from-gitlab.sh [environment]
```

**Arguments:**
- `environment`: `production` (default) or `staging`

**What it does:**
1. Validates GitLab token
2. Resolves project ID from git remote
3. Fetches Terraform state from GitLab API
4. Extracts outputs from state
5. Saves to `terraform/outputs.json`

**Requirements:**
- `GITLAB_PAT` environment variable
- Git repository with GitLab remote
- Terraform state must exist in GitLab

### 2. `generate-inventory-auto.sh`

**Location:** `ansible-ci/scripts/generate-inventory-auto.sh`

**Purpose:** Generates Ansible inventory (auto-detects GitLab or local)

**Usage:**
```bash
./generate-inventory-auto.sh
```

**What it does:**
1. Checks if `outputs.json` exists locally
2. If not, prompts to fetch from GitLab
3. Generates `inventory.ini` from outputs
4. Displays cluster summary

**Benefits:**
- Automatic detection of outputs location
- Interactive prompts for environment selection
- Works with both GitLab and local Terraform

### 3. `generate-inventory-from-terraform.sh`

**Location:** `ansible-ci/scripts/generate-inventory-from-terraform.sh`

**Purpose:** Generates inventory from local `outputs.json` only

**Usage:**
```bash
./generate-inventory-from-terraform.sh
```

**Note:** This is the original script and requires local `outputs.json`

## Complete Workflow Example

### Scenario: After GitLab CI/CD Terraform Apply

```bash
# 1. Set GitLab credentials
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"

# 2. Fetch outputs from GitLab
cd scripts
./get-terraform-outputs-from-gitlab.sh production
# Output: ✅ Outputs saved to: ../terraform/outputs.json

# 3. Generate Ansible inventory
cd ../ansible-ci
./scripts/generate-inventory-auto.sh
# Output: ✅ Ansible inventory generated successfully!

# 4. Test connectivity
ansible k8s-master -i inventory.ini -m ping
# Output: k8s-master | SUCCESS => { ... }

# 5. Bootstrap Kubernetes
ansible-playbook -i inventory.ini k8s-bootstrap.yml

# 6. Deploy applications
export POSTGRES_PASSWORD="your-password"
export JWT_SECRET="your-jwt-secret"
ansible-playbook -i inventory.ini deploy-apps.yml

# 7. Check status
ansible-playbook -i inventory.ini status.yml
```

## Troubleshooting

### Issue: "Could not find project ID"

**Solution:**
```bash
# Option 1: Set manually
export PROJECT_ID="your-project-id"
./get-terraform-outputs-from-gitlab.sh production

# Option 2: Check git remote
git remote get-url origin
# Ensure you're in a GitLab repository
```

### Issue: "Failed to fetch Terraform state"

**Possible causes:**
1. Wrong environment name
2. Insufficient permissions
3. Terraform not applied yet

**Solution:**
```bash
# Check environment name
# Should be 'production' or 'staging'

# Check token permissions
# Token must have 'api' scope

# Check if Terraform was applied
# Go to GitLab: Project → Infrastructure → Terraform
```

### Issue: "No outputs found in Terraform state"

**Possible causes:**
1. Terraform apply failed
2. No outputs defined
3. Wrong environment

**Solution:**
```bash
# Check GitLab CI/CD pipeline
# View Terraform job logs in GitLab UI

# Verify outputs are defined
cat terraform/outputs.tf
```

### Issue: "ansible k8s-master -m ping fails"

**Possible causes:**
1. SSH key not configured
2. Wrong IP address
3. Firewall blocking SSH

**Solution:**
```bash
# Test SSH manually
ssh -i ~/.ssh/id_rsa ajasta@<master-ip>

# Check if VMs are running
yc compute instance list

# Verify IP in outputs.json
cat terraform/outputs.json | jq '.master_public_ip'
```

## GitLab CI/CD Integration

The fetch scripts work seamlessly with GitLab CI/CD:

```yaml
# Example GitLab CI job
fetch-terraform-outputs:
  stage: prepare
  script:
    - ./scripts/get-terraform-outputs-from-gitlab.sh production
  artifacts:
    paths:
      - terraform/outputs.json
    expire_in: 1 day
```

## Environment Variables Reference

### Required

- `GITLAB_PAT` - GitLab Personal Access Token with `api` scope

### Optional

- `PROJECT_ID` - GitLab project numeric ID (auto-detected if not set)
- `CI_API_V4_URL` - GitLab API URL (default: https://gitlab.com/api/v4)
- `CI_PROJECT_ID` - GitLab project ID (auto-detected if not set)

## Security Best Practices

1. **Never commit GITLAB_PAT** to repository
   ```bash
   # Add to .gitignore
   echo "GITLAB_PAT" >> .gitignore
   ```

2. **Use environment variables**
   ```bash
   # Set in shell profile
   export GITLAB_PAT="glpat-xxx"
   ```

3. **Token rotation**
   - Rotate tokens regularly
   - Use different tokens for different environments

4. **Token scope minimization**
   - Use minimum required scope (`api` for this workflow)

## FAQ

### Q: Why do I need this?

A: When Terraform runs in GitLab CI/CD with HTTP backend, the state is stored in GitLab, not locally. Ansible needs `outputs.json` to know the VM IPs.

### Q: Can I use local Terraform instead?

A: Yes! If you run `terraform apply` locally, it will create `outputs.json` and Ansible can use it directly.

### Q: How often should I fetch outputs?

A: Only when:
- Terraform has been applied via GitLab CI/CD
- You need to regenerate Ansible inventory
- VM IPs have changed

### Q: Is this secure?

A: Yes. The script uses your GitLab PAT to access the Terraform state, which is already secured by GitLab. Ensure your PAT has the minimum required scope (`api`).

### Q: What if I have multiple environments?

A: Specify the environment when fetching:
```bash
./get-terraform-outputs-from-gitlab.sh production
./get-terraform-outputs-from-gitlab.sh staging
```

## Comparison: GitLab vs Local Terraform

| Aspect | GitLab Terraform | Local Terraform |
|--------|-----------------|-----------------|
| **State Storage** | GitLab HTTP backend | Local file |
| **outputs.json** | Needs fetch script | Auto-generated |
| **CI/CD** | Integrated | Manual |
| **Collaboration** | Team-friendly | Individual |
| **Use Case** | Production teams | Local development |

## Summary

1. **Use `generate-inventory-auto.sh`** for automatic detection
2. **Use `get-terraform-outputs-from-gitlab.sh`** for manual fetch
3. **Set `GITLAB_PAT`** before running scripts
4. **Choose environment** (production/staging)
5. **Test connectivity** after generating inventory

This workflow bridges the gap between GitLab CI/CD Terraform and local Ansible automation.

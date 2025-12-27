# GitLab Terraform Integration - Complete Working Solution

## Overview

This document describes the complete working solution for fetching Terraform outputs from GitLab's HTTP backend and generating Ansible inventory for local Kubernetes bootstrapping and application deployment.

## Problem Solved

When Terraform is executed via GitLab CI/CD with the HTTP backend, the Terraform state is stored in GitLab, not locally. This prevented Ansible from generating inventory because `terraform/outputs.json` didn't exist on the local machine.

## Solution Components

### 1. Fetch Script: `scripts/get-terraform-outputs-from-gitlab.sh`

Fetches Terraform outputs from GitLab HTTP backend and saves them locally.

**Key Features:**
- Auto-detects GitLab instance from git remote (supports gitlab.com and self-hosted)
- Supports both manual PROJECT_ID setting and auto-detection
- Interactive prompt when PROJECT_ID auto-detection fails
- Handles outputs in both direct format and nested `.value` format
- Works with self-hosted GitLab instances

**Usage:**
```bash
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"
export PROJECT_ID="1305"  # Optional: auto-detected if possible
cd scripts
./get-terraform-outputs-from-gitlab.sh production
```

**What It Does:**
1. Validates prerequisites (jq, curl, GITLAB_PAT)
2. Detects GitLab instance from git remote URL
3. Resolves project ID (auto or manual)
4. Fetches Terraform state from GitLab API
5. Extracts outputs from state response
6. Saves to `terraform/outputs.json`

### 2. Inventory Generator: `ansible-ci/scripts/generate-inventory-from-terraform.sh`

Generates Ansible inventory from Terraform outputs.

**Key Features:**
- Reads `terraform/outputs.json`
- Handles both GitLab format (nested `.value`) and direct format
- Creates `ansible-ci/inventory.ini` with all cluster nodes
- Displays cluster summary

**Usage:**
```bash
cd ansible-ci
./scripts/generate-inventory-from-terraform.sh
```

### 3. Automated Script: `ansible-ci/scripts/generate-inventory-auto.sh`

Convenience script that automatically detects if outputs need to be fetched.

**Key Features:**
- Checks if `outputs.json` exists locally
- Prompts to fetch from GitLab if not found
- Handles environment selection (production/staging)
- Generates inventory automatically

**Usage:**
```bash
cd ansible-ci
./scripts/generate-inventory-auto.sh
```

## Technical Details

### GitLab State Response Format

The GitLab Terraform HTTP state API returns outputs in one of two formats:

**Format 1: Direct Outputs (Newer GitLab versions)**
```json
{
  "version": 4,
  "terraform_version": "1.13.5",
  "outputs": {
    "master_public_ip": {"value": "158.160.85.51", "type": "string", ...},
    "worker_public_ips": {"value": {...}, "type": "object", ...}
  }
}
```

**Format 2: Download URL (Older GitLab versions)**
```json
{
  "links": {
    "state-download": "https://gitlab.example.com/..."
  }
}
```

### Output Extraction Logic

The fetch script handles both formats:

```bash
# Check if outputs are directly in the response
DIRECT_OUTPUTS=$(echo "${STATE_RESPONSE}" | jq -r '.outputs // empty')

if [[ -n "${DIRECT_OUTPUTS}" ]] && [[ "${DIRECT_OUTPUTS}" != "null" ]]; then
    log_info "Outputs found directly in state response (newer GitLab version)"
    OUTPUTS="${DIRECT_OUTPUTS}"
else
    # Try to get state download URL (older GitLab versions)
    STATE_DOWNLOAD_URL=$(echo "${STATE_RESPONSE}" | jq -r '.links["state-download"] // empty')
    # ... download logic
fi

# Convert outputs to JSON format (extract .value from nested structure)
OUTPUTS_JSON=$(echo "${OUTPUTS}" | jq 'with_entries(.value = .value.value)')
```

### Inventory Generation Logic

The inventory script handles both output formats:

```bash
# Extract values from either format:
# GitLab format: "master_public_ip": {"value": "1.2.3.4", ...}
# Direct format:  "master_public_ip": "1.2.3.4"

MASTER_IP=$(jq -r '.master_public_ip | if type == "object" and has("value") then .value elif type == "string" then . else . end' "${OUTPUTS_FILE}")
```

This jq query:
1. Checks if the value is an object with a `.value` field → extracts `.value`
2. Checks if the value is a string → uses it directly
3. Otherwise → uses the value as-is

## Complete Workflow

### Scenario: After GitLab CI/CD Terraform Apply

```bash
# 1. Set GitLab credentials
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"
export PROJECT_ID="1305"  # Optional: can be auto-detected

# 2. Fetch outputs from GitLab
cd scripts
./get-terraform-outputs-from-gitlab.sh production

# Output:
# ✅ Terraform state fetched successfully
# ℹ️  Outputs found directly in state response (newer GitLab version)
# ✅ Outputs saved to: ../terraform/outputs.json
# Master IP: 158.160.85.51
# Worker IPs:
#   k8s-worker-1: 158.160.91.237
#   k8s-worker-2: 84.252.142.237
#   k8s-worker-3: 158.160.88.103
# ✅ Total nodes: 4

# 3. Generate Ansible inventory
cd ../ansible-ci
./scripts/generate-inventory-from-terraform.sh

# Output:
# ✅ Ansible inventory generated successfully!
# Cluster nodes:
#   Master:  158.160.85.51
#   Workers: 3 node(s)
#     k8s-worker-0 ansible_host=158.160.91.237
#     k8s-worker-1 ansible_host=84.252.142.237
#     k8s-worker-2 ansible_host=158.160.88.103

# 4. Test connectivity
ansible k8s-master -i inventory.ini -m ping
ansible k8s -i inventory.ini -m ping

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

### Issue: "Could not auto-detect project ID"

**Solution:**
```bash
# Set PROJECT_ID manually
export PROJECT_ID="1305"
./get-terraform-outputs-from-gitlab.sh production
```

### Issue: "GITLAB_PAT environment variable not set"

**Solution:**
```bash
# For zsh (use double quotes!)
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"

# Verify
echo "$GITLAB_PAT"  # Should show your token, not literal "$GITLAB_PAT"
```

### Issue: "jq: error (at outputs.json:37): Cannot index string with string 'value'"

**Solution:** This error is fixed in the latest version. Update the scripts to use the type-checking logic:
```bash
MASTER_IP=$(jq -r '.master_public_ip | if type == "object" and has("value") then .value elif type == "string" then . else . end' "${OUTPUTS_FILE}")
```

### Issue: "SSH connection refused"

**Solution:**
```bash
# Test SSH manually
ssh -i ~/.ssh/id_rsa ajasta@<master-ip>

# Check if VMs are running
yc compute instance list

# Verify IPs in outputs.json
cat terraform/outputs.json | jq '.master_public_ip'
```

## Self-Hosted GitLab Support

The scripts fully support self-hosted GitLab instances:

```bash
# Example: otusteam.gitlab.yandexcloud.net
git remote get-url origin
# Output: https://otusteam.gitlab.yandexcloud.net/Vladimir.Rurik/ajasta-app.git

# Script auto-detects:
# - GITLAB_HOST: otusteam.gitlab.yandexcloud.net
# - GITLAB_API_URL: https://otusteam.gitlab.yandexcloud.net/api/v4
# - GITLAB_PATH: Vladimir.Rurik/ajasta-app
```

No configuration needed - the script automatically detects from git remote!

## Environment Variables Reference

### Required

- `GITLAB_PAT` - GitLab Personal Access Token with `api` scope
  - Create at: https://gitlab.com/-/user_settings/personal_access_tokens (or self-hosted equivalent)
  - Required scope: **api**

### Optional

- `PROJECT_ID` - GitLab project numeric ID
  - Can be auto-detected from git remote
  - Manual setting may be required for self-hosted instances
  - Find in GitLab UI when navigating to project pages

## Security Best Practices

1. **Never commit GITLAB_PAT** to repository
   ```bash
   # Add to .gitignore
   echo "GITLAB_PAT" >> .gitignore
   ```

2. **Use environment variables**
   ```bash
   # Set in shell profile (~/.zshrc or ~/.bashrc)
   export GITLAB_PAT="glpat-xxx"
   ```

3. **Token rotation**
   - Rotate tokens regularly
   - Use different tokens for different environments

4. **Token scope minimization**
   - Use minimum required scope (`api` for this workflow)

## Summary

This solution provides a complete, automated workflow for:

1. ✅ Fetching Terraform outputs from GitLab HTTP backend
2. ✅ Generating Ansible inventory from fetched outputs
3. ✅ Supporting both gitlab.com and self-hosted instances
4. ✅ Handling multiple output format variations
5. ✅ Providing clear error messages and troubleshooting

The workflow bridges the gap between GitLab CI/CD Terraform and local Ansible automation, enabling seamless local development and testing of infrastructure deployed via CI/CD.

## Quick Reference

```bash
# One-command workflow
export GITLAB_PAT="glpat-xxx" PROJECT_ID="1305" && \
cd scripts && ./get-terraform-outputs-from-gitlab.sh production && \
cd ../ansible-ci && ./scripts/generate-inventory-from-terraform.sh && \
ansible-playbook -i inventory.ini k8s-bootstrap.yml
```

## Related Documentation

- `ansible-ci/GITLAB_TERRAFORM_WORKFLOW.md` - Detailed workflow guide
- `ansible-ci/README.md` - Ansible CI usage and playbooks
- `terraform/GITLAB_CI_TERRAFORM_SETUP.md` - GitLab CI/CD Terraform setup
- `scripts/get-terraform-outputs-from-gitlab.sh` - Fetch script source
- `ansible-ci/scripts/generate-inventory-from-terraform.sh` - Inventory generator source

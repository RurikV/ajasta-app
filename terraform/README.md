# Terraform Multi-Environment Setup for Ajasta App

This directory contains Terraform configuration for deploying Ajasta App infrastructure in Yandex Cloud with support for multiple environments (staging/production) and remote backend for state management.

## Environment Architecture

### Supported Environments
- **staging** - testing environment for development and testing
- **production** - production environment for live deployment
- **default** - default environment for local development

### Key Infrastructure Components
- **VPC Networks**: external and internal networks for each environment
- **Subnets**: external and internal subnets in a single availability zone
- **Static IP Addresses**: reserved public IPs for master and worker nodes
- **Compute Instances**: 1 master and N worker nodes with NAT
- **Service Account**: optional Yandex Cloud service account
- **Remote Backend**: HTTP backend in GitLab for state storage

### Environment-to-Code Binding

Each environment is isolated using:
1. **Terraform Workspaces** - separate workspace for each environment
2. **Separate Variable Files**:
   - `staging.tfvars` - variables for staging environment
   - `production.tfvars` - variables for production environment
   - `terraform.tfvars` - common default variables
3. **Isolated State Files** - each environment stores its state in separate files
4. **Different Resource Prefixes** - resources of different environments have unique names

### Environment Configuration

#### Staging Environment
- **Resources**: minimal for cost optimization
- **Virtual Machines**: 1 master + 1 worker
- **Performance**: 2 vCPU, 4 GB RAM, 20 GB disk
- **Preemptible**: yes (for cost savings)
- **Network**: 172.16.18.0/28 (external), 10.10.1.0/24 (internal)
- **Application**: 1 replica with minimal resources

#### Production Environment
- **Resources**: high-availability configuration
- **Virtual Machines**: 1 master + 3 workers
- **Performance**: 4 vCPU, 8 GB RAM, 50 GB SSD disk
- **Preemptible**: no (for stability)
- **Network**: 172.16.19.0/28 (external), 10.10.2.0/24 (internal)
- **Application**: 3 replicas with enhanced resources

## Remote Backend Configuration

### GitLab HTTP Backend
- **Type**: HTTP backend integrated with GitLab CI/CD
- **State Storage**: GitLab Projects → Settings → Repository → Terraform state
- **Locking**: built-in state locking via GitLab API
- **Isolation**: each workspace uses separate state file
- **Automation**: automatic state creation on first run

### State Files
- **Staging**: `gitlab-state-staging-{pipeline_id}`
- **Production**: `gitlab-state-production-{pipeline_id}`
- **Default**: `gitlab-state-default-{pipeline_id}`

## Environment Management

### Using Management Script

For convenient environment management, use the `manage-workspaces.sh` script:

```bash
# Initialize Terraform
./manage-workspaces.sh init

# Create environments
./manage-workspaces.sh create staging
./manage-workspaces.sh create production

# Switch between environments
./manage-workspaces.sh select staging
./manage-workspaces.sh select production

# Plan changes
./manage-workspaces.sh plan staging
./manage-workspaces.sh plan production

# Apply changes
./manage-workspaces.sh apply staging
./manage-workspaces.sh apply production

# View environment status
./manage-workspaces.sh status

# List all environments
./manage-workspaces.sh list

# Delete environment (careful!)
./manage-workspaces.sh delete staging
```

### Manual Workspace Management

```bash
# Initialize
terraform init

# Create new workspaces
terraform workspace new staging
terraform workspace new production

# Switch between workspaces
terraform workspace select staging
terraform workspace select production

# Apply with environment variables
terraform workspace select staging
terraform apply -var-file="staging.tfvars"

terraform workspace select production
terraform apply -var-file="production.tfvars"
```

## File Structure

### Main Configuration Files
- `versions.tf` — Terraform and provider versions
- `providers.tf` — Yandex provider configuration
- `variables.tf` — input variables
- `workspaces.tf` — workspace and environment configuration
- `backend.tf` — remote backend configuration

### Infrastructure Files
- `network.tf` — networks and subnets
- `compute.tf` — compute instances
- `addresses.tf` — static IP addresses
- `service_account.tf` — service accounts
- `outputs.tf` — output variables
- `whoami.tf` — diagnostic information

### Environment Files
- `terraform.tfvars` — default variables
- `terraform.tfvars.example` — variable examples
- `staging.tfvars` — staging variables
- `production.tfvars` — production variables

### Scripts and Automation
- `manage-workspaces.sh` — workspace management script
- `terraform.tfstate.example` — example state file (no real resources)

## Quick Start

### 1. Environment Setup
```bash
# Copy and edit variables file
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Set yc_cloud_id, yc_folder_id, ssh_public_key and other variables
```

### 2. Initialization and Workspace Creation
```bash
# Initialize Terraform
./manage-workspaces.sh init

# Create staging environment
./manage-workspaces.sh create staging

# Switch to staging environment
./manage-workspaces.sh select staging
```

### 3. Infrastructure Deployment
```bash
# Plan changes
./manage-workspaces.sh plan staging

# Apply changes
./manage-workspaces.sh apply staging
```

## Yandex Cloud Authentication

### Recommended Method: YC_TOKEN
```bash
# Authenticate via Yandex Cloud CLI
yc init

# Get token
yc iam create-token | pbcopy

# Export variable
export YC_TOKEN="your-token-here"
```

### Alternative Method: Service Account Key
```bash
# Create service account
yc iam service-account create --name ajasta-tf

# Create IAM key
yc iam key create --service-account-id <sa-id> --output sa-iam-key.json

# Specify key path in terraform.tfvars
yc_service_account_key_file = "sa-iam-key.json"
```

## Required Service Account Permissions

For Terraform to work, the following permissions are required on the folder:
- `vpc.admin` — create VPC networks and subnets
- `vpc.publicAdmin` — allocate static public IP addresses
- `compute.editor` or `compute.admin` — create VMs and disks

## CI/CD Integration

The project is integrated with GitLab CI/CD via `.gitlab-ci-terraform.yml`:
- Automatic state file creation
- Different pipelines for staging and production
- Automatic state locking/unlocking
- Artifact saving of plans and results

## Ansible Compatibility

Configuration is compatible with existing scripts:
- Preemptible instances with `core_fraction = 20`
- Boot disk type `network-hdd`
- Image `centos-stream-9-oslogin`
- Cloud-init from `../scripts/metadata.yaml`
- SSH key injection via variables

## Output Variables

- `master_public_ip` — master public IP
- `worker_public_ips` — map of worker IPs
- `current_workspace` — current workspace
- `environment_config` — current environment configuration

## Infrastructure Removal

```bash
# Switch to desired environment
./manage-workspaces.sh select staging

# Remove resources
terraform destroy

# Or delete entire workspace (careful!)
./manage-workspaces.sh delete staging
```

## Troubleshooting

### whoami diagnostic
To check current context (cloud/folder/zone), use output `current_context`, which is visible in plan/apply output.

### State Cleanup
```bash
# Clear cache and state
rm -rf .terraform .terraform.lock.hcl
terraform init
```

### Working with Locks
```bash
# Force unlock state
terraform force-unlock LOCK_ID

# View lock information
terraform force-unlock -help
```

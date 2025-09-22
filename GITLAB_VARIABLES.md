# GitLab CI/CD Variables Configuration

This document describes the required GitLab CI/CD variables for the Ajasta App deployment pipeline.

## Required Variables

### Yandex Cloud Configuration

Set these variables in GitLab Project Settings > CI/CD > Variables:

| Variable | Description | Required | Type | Example |
|----------|-------------|----------|------|---------|
| `YC_TOKEN` | Yandex Cloud OAuth token | Yes | Protected | `AQAAAABq...` |
| `YC_CLOUD_ID` | Yandex Cloud ID | Yes | Protected | `b1g2a3b4c5d6` |
| `YC_FOLDER_ID` | Yandex Cloud Folder ID | Yes | Protected | `b1g7h8i9j0k1` |
| `YC_VM_EXTERNAL_IP` | VM External IP (set after first deployment) | No | Protected | `123.45.67.89` |

### GitLab Registry Configuration

These are automatically provided by GitLab:

| Variable | Description | Auto-Generated |
|----------|-------------|----------------|
| `CI_REGISTRY` | GitLab Container Registry URL | Yes |
| `CI_REGISTRY_USER` | Registry username | Yes |
| `CI_REGISTRY_PASSWORD` | Registry password | Yes |

### SSH Configuration

| Variable | Description | Required | Type |
|----------|-------------|----------|------|
| `YC_SSH_PRIVATE_KEY` | SSH private key for VM access | Yes | File |

## Optional Variables

### Environment Overrides

| Variable | Description | Default | Type |
|----------|-------------|---------|------|
| `YC_ZONE` | Yandex Cloud zone | `ru-central1-a` | Variable |
| `YC_INSTANCE_NAME` | VM instance name | `ajasta-app-vm` | Variable |
| `YC_CORES` | VM CPU cores | `2` | Variable |
| `YC_MEMORY` | VM memory | `4GB` | Variable |
| `YC_DISK_SIZE` | VM disk size | `20GB` | Variable |

### Application Configuration

| Variable | Description | Default | Type |
|----------|-------------|---------|------|
| `POSTGRES_DB` | Database name | `ajastadb` | Variable |
| `POSTGRES_USER` | Database user | `admin` | Variable |
| `POSTGRES_PASSWORD` | Database password | `adminpw` | Protected |
| `JWT_SECRET` | JWT secret key | `change-me-production-secret-key` | Protected |

## Setup Instructions

### 1. Yandex Cloud Setup

1. **Get OAuth Token:**
   ```bash
   yc iam create-token
   ```

2. **Get Cloud and Folder IDs:**
   ```bash
   yc config list
   ```

3. **Set variables in GitLab:**
   - Go to Project Settings > CI/CD > Variables
   - Add each variable with appropriate protection settings

### 2. SSH Key Setup

1. **Generate SSH key pair:**
   ```bash
   ssh-keygen -t rsa -b 2048 -f ~/.ssh/yc_deploy_key -N ""
   ```

2. **Add private key to GitLab:**
   - Variable name: `YC_SSH_PRIVATE_KEY`
   - Type: File
   - Value: Content of `~/.ssh/yc_deploy_key`
   - Protected: Yes

### 3. First Deployment

1. **Initial deployment creates VM:**
   - Pipeline will create VM and infrastructure
   - Note the external IP from deployment logs

2. **Update VM IP variable:**
   - Add `YC_VM_EXTERNAL_IP` variable with the external IP
   - This enables proper frontend API configuration

### 4. Registry Access

Ensure your GitLab project has Container Registry enabled:
- Go to Project Settings > General > Visibility
- Enable Container Registry

## Variable Scopes

### Protected Variables
Use for production deployments (protected branches):
- `YC_TOKEN`
- `YC_CLOUD_ID` 
- `YC_FOLDER_ID`
- `YC_SSH_PRIVATE_KEY`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`

### Masked Variables
Hide values in job logs:
- `YC_TOKEN`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`

## Environment-Specific Configuration

### Staging Environment
- Uses `develop` branch
- Can use separate Yandex Cloud folder
- Different VM instance name: `ajasta-app-staging-vm`

### Production Environment
- Uses `main` branch and tags
- Protected variables required
- Manual deployment approval

## Troubleshooting

### Common Issues

1. **Authentication Failed:**
   - Check `YC_TOKEN` is valid and not expired
   - Verify cloud and folder IDs are correct

2. **SSH Connection Failed:**
   - Verify SSH private key format (no carriage returns)
   - Check VM security group allows SSH (port 22)

3. **Registry Access Denied:**
   - Ensure Container Registry is enabled
   - Check service account has `container-registry.images.puller` role

### Validation Commands

Test your configuration locally:

```bash
# Test Yandex Cloud authentication
yc config set token $YC_TOKEN
yc config set cloud-id $YC_CLOUD_ID
yc config set folder-id $YC_FOLDER_ID
yc compute instance list

# Test SSH key
ssh-keygen -l -f ~/.ssh/yc_deploy_key

# Test Docker registry access
echo $CI_REGISTRY_PASSWORD | docker login -u $CI_REGISTRY_USER --password-stdin $CI_REGISTRY
```

## Security Best Practices

1. **Use minimum required permissions**
2. **Enable variable protection for production**
3. **Mask sensitive variables**
4. **Rotate tokens and keys regularly**
5. **Use separate credentials for staging/production**
6. **Monitor access logs**

## Pipeline Stages

The pipeline will use these variables across different stages:

- **Validate:** Syntax and Docker validation
- **Build:** Docker image building and registry push
- **Test:** Automated testing (no cloud variables needed)
- **Package:** Create deployment artifacts
- **Deploy:** Yandex Cloud deployment (all variables required)

Make sure all required variables are set before running the pipeline.
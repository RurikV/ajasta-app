# Terraform Deployment Troubleshooting Guide

## 🚨 Current Issue: Resource Conflicts & Quota Limits

Your Terraform deployment is failing because:

1. **Existing Resources**: Static IPs with names `ajasta-k8s-master-ip` and `ajasta-k8s-workerX-ip` already exist
2. **Quota Exceeded**: You've hit the VPC networks count limit in Yandex Cloud
3. **State Mismatch**: Terraform state doesn't track existing resources

## 🛠️ **Immediate Solution**

### Option 1: Clean Up Existing Resources (Recommended)

```bash
# 1. Install and configure Yandex Cloud CLI
curl -sSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash
export PATH=$PATH:/root/yandex-cloud/bin
yc init

# 2. Run the cleanup script
./scripts/cleanup-yandex-resources.sh cleanup

# 3. Alternatively, use the interactive mode
./scripts/cleanup-yandex-resources.sh
# Choose option 5 for full cleanup (type 'DELETE' to confirm)
```

### Option 2: Complete Terraform Reset

```bash
# Complete reset including Terraform state
./scripts/terraform-destroy-all.sh --force
```

### Option 3: Manual Cleanup in Yandex Cloud Console

1. Go to [Yandex Cloud Console](https://console.cloud.yandex.ru/)
2. Navigate to your folder
3. Delete these resources:
   - **VPC → Addresses**: `ajasta-k8s-master-ip`, `ajasta-k8s-worker1-ip`, `ajasta-k8s-worker2-ip`, `ajasta-k8s-worker3-ip`
   - **VPC → Networks**: `external-ajasta-network`, `internal-ajasta-network`
   - **Compute → VMs**: Any `k8s-*` instances

## 🔄 **After Cleanup: Re-run Deployment**

Once cleanup is complete:

1. **GitLab CI/CD**: The pipeline should now work automatically
2. **Manual deployment**:
   ```bash
   cd terraform
   terraform init
   terraform plan
   terraform apply
   ```

## 📊 **Check Quota Usage**

```bash
# Check current resource usage
./scripts/cleanup-yandex-resources.sh quotas

# To request quota increases:
# Visit: https://console.cloud.yandex.ru/folders/YOUR_FOLDER_ID/quotas
```

## 🆕 **Improved GitLab CI/CD Features**

We've enhanced your pipeline to handle these scenarios:

### **New Jobs Added:**
1. **`terraform:cleanup:dev`** - Manual cleanup job for development environment
2. **Better error handling** in apply jobs with clear diagnostics
3. **Graceful failure modes** with useful output files

### **Enhanced Error Handling:**
- Always generates `terraform_outputs.env` (even on failure)
- Provides detailed error messages and troubleshooting hints
- Automatic fallback to manual cleanup if Terraform destruction fails

### **In Your GitLab UI:**
1. Go to your project → **CI/CD → Pipelines**
2. If a Terraform job fails, run the **`terraform:cleanup:dev`** job
3. Then re-trigger the pipeline

## 🧩 **Understanding the Issues**

### **Resource Conflicts**
- Happens when previous deployments didn't clean up properly
- Resources exist in Yandex Cloud but not in Terraform state
- Solution: Clean up existing resources before redeployment

### **Quota Limits**
- Yandex Cloud has limits on resource counts per folder
- Common limits: VPC networks, static IPs, VMs
- Solution: Clean up unused resources or request quota increases

### **State Management**
- Terraform tracks resources in state files
- If state is lost or corrupted, Terraform can't manage existing resources
- Solution: Complete reset with `terraform-destroy-all.sh`

## 🚀 **Prevention Tips**

### **For Future Deployments:**
1. **Always use cleanup** before major changes
2. **Monitor quota usage** regularly
3. **Use environment-specific resource names** to avoid conflicts
4. **Backup Terraform state** for critical environments

### **Best Practices:**
```bash
# Before making changes:
./scripts/cleanup-yandex-resources.sh list  # See what exists

# After failed deployments:
./scripts/terraform-destroy-all.sh --force  # Clean slate

# Regular maintenance:
./scripts/cleanup-yandex-resources.sh quotas  # Check usage
```

## 🆘 **Still Having Issues?**

1. **Check Yandex Cloud Console** for stuck resources
2. **Verify folder permissions** and quotas
3. **Contact Yandex Cloud support** for quota increases
4. **Review Terraform state** with `terraform state list`

The cleanup scripts are designed to handle the most common scenarios and should resolve your current deployment issues!
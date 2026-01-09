# ✅ Kubeconfig Auto-Update Solution - Complete!

## What Was Created

### 1. **Main Script** - `update-kubeconfig.sh`
Automatically updates `~/.kube/config` with the current master IP from Terraform.

**Location:** `ansible-ci/scripts/update-kubeconfig.sh`

**Features:**
- ✅ Reads master IP from `terraform/outputs.json`
- ✅ SSH's to master and fetches admin kubeconfig
- ✅ Creates timestamped backups
- ✅ Smart IP change detection
- ✅ Verifies connectivity after update
- ✅ Safe to run multiple times

**Usage:**
```bash
cd ansible-ci
./scripts/update-kubeconfig.sh
```

### 2. **Convenience Wrapper** - `post-terraform-setup.sh`
Runs inventory generation + kubeconfig update in one command.

**Location:** `ansible-ci/scripts/post-terraform-setup.sh`

**Usage:**
```bash
cd ansible-ci
./scripts/post-terraform-setup.sh
```

### 3. **Shell Aliases** - `shell-aliases.sh`
Quick aliases for common operations.

**Location:** `ansible-ci/scripts/shell-aliases.sh`

**Setup:**
```bash
# Add to your ~/.bashrc or ~/.zshrc:
source ~/IdeaProjects/petrelevich/ajasta-app/ansible-ci/scripts/shell-aliases.sh
```

**Commands:**
```bash
aj-k8s-info           # Show cluster info
aj-k8s-update         # Update kubeconfig
aj-k8s-full-update    # Full environment update
aj-k8s-status         # Show node status
aj-k8s-pods           # Show all pods
```

### 4. **Ansible Integration**
The `k8s-bootstrap.yml` playbook now automatically updates your local kubeconfig at the end!

**No manual steps needed!** Just run:
```bash
ansible-playbook -i inventory.ini k8s-bootstrap.yml
# ✓ Updates kubeconfig automatically at the end
```

## Quick Start

### After Terraform Apply

**Option 1: Let Ansible do it (Recommended)**
```bash
cd ansible-ci
ansible-playbook -i inventory.ini k8s-bootstrap.yml
# Kubeconfig is updated automatically! ✓
```

**Option 2: Manual update**
```bash
cd ansible-ci
./scripts/update-kubeconfig.sh
```

**Option 3: Quick alias**
```bash
# After loading aliases (see below)
aj-k8s-update
```

### Setup Shell Aliases (Optional)

Add to your `~/.zshrc` or `~/.bashrc`:

```bash
# Ajasta Kubernetes aliases
source ~/IdeaProjects/petrelevich/ajasta-app/ansible-ci/scripts/shell-aliases.sh
```

Then reload:
```bash
source ~/.zshrc  # or source ~/.bashrc
```

Now you can use:
```bash
aj-k8s-info        # Show cluster info and current master IP
aj-k8s-update      # Update kubeconfig
aj-k8s-full-update # Full environment update
```

## How It Works

### The Problem

Every time you run Terraform:
1. Old VMs are destroyed
2. New VMs are created with **new IPs**
3. Your `~/.kube/config` still points to **old master IP**
4. `kubectl` commands fail ❌

### The Solution

The update script:
1. Reads current master IP from `terraform/outputs.json`
2. SSH's to master and fetches `/etc/kubernetes/admin.conf`
3. Updates `~/.kube/config` with new server IP
4. Backs up old config automatically
5. Verifies connectivity with `kubectl get nodes`

### Workflow

```
Terraform Apply (New IPs!)
       ↓
Generate outputs.json
       ↓
Run update-kubeconfig.sh
       ↓
~/.kube/config updated ✓
       ↓
kubectl works with new cluster!
```

## Examples

### Example 1: After Terraform Apply

```bash
# Terraform creates new cluster with new IPs
cd terraform
terraform apply
# Master IP changed from 158.160.79.42 → 158.160.95.123

# Update kubeconfig
cd ../ansible-ci
./scripts/update-kubeconfig.sh
# ✓ Detected IP change: 158.160.79.42 → 158.160.95.123
# ✓ Kubeconfig updated successfully
# ✓ Successfully connected to cluster!

# Verify
kubectl get nodes
# Works with new IP! ✓
```

### Example 2: Using Ansible (Automatic)

```bash
# Bootstrap cluster
cd ansible-ci
ansible-playbook -i inventory.ini k8s-bootstrap.yml

# ... 15-20 minutes later ...

# Playbook ends with:
# ✅ Kubernetes Cluster Bootstrap Complete!
# 🔄 Updating local kubeconfig...
# ✓ Kubeconfig updated successfully
# ✓ Successfully connected to cluster!

# kubectl works immediately!
kubectl get nodes
```

### Example 3: Using Shell Aliases

```bash
# Load aliases first (add to ~/.zshrc)
source ~/IdeaProjects/petrelevich/ajasta-app/ansible-ci/scripts/shell-aliases.sh

# Quick cluster info
aj-k8s-info
# === Ajasta Kubernetes Cluster Info ===
# Master IP: 158.160.95.123
# Kubeconfig: /Users/rurik/.kube/config
# Cluster Status: Kubernetes control plane is running
# Nodes: 4 nodes (1 master + 3 workers)

# Update after Terraform
aj-k8s-update
# ✓ Kubeconfig updated successfully

# Check status
aj-k8s-status
# NAME           STATUS   ROLES           AGE   VERSION
# k8s-master     Ready    control-plane   5m    v1.29.x
# k8s-worker-0   Ready    <none>          4m    v1.29.x
# k8s-worker-1   Ready    <none>          4m    v1.29.x
# k8s-worker-2   Ready    <none>          4m    v1.29.x
```

## Script Options

### update-kubeconfig.sh Options

```bash
# Print master IP only (don't update)
./scripts/update-kubeconfig.sh --print-ip-only

# Force update even if IP hasn't changed
./scripts/update-kubeconfig.sh --force

# Update existing kubeconfig without SSH'ing to master
./scripts/update-kubeconfig.sh --skip-ssh

# Show help
./scripts/update-kubeconfig.sh --help
```

## Safety Features

- ✅ **Automatic backups** - Creates timestamped backups before updating
- ✅ **Smart detection** - Only updates if IP actually changed
- ✅ **Connectivity check** - Verifies cluster is accessible
- ✅ **Non-destructive** - Never deletes, only replaces
- ✅ **Clear error messages** - Tells you exactly what went wrong

## Files Created

1. `ansible-ci/scripts/update-kubeconfig.sh` - Main update script
2. `ansible-ci/scripts/post-terraform-setup.sh` - Convenience wrapper
3. `ansible-ci/scripts/shell-aliases.sh` - Shell aliases for quick access
4. `ansible-ci/KUBECONFIG_UPDATE_GUIDE.md` - Complete documentation

## Integration Points

### Ansible Playbooks
- `k8s-bootstrap.yml` - Automatically runs update at end
- Future playbooks can call the script too

### Terraform Pipeline
- Can be added as a post-apply step in CI/CD
- Run manually after local Terraform applies

## Troubleshooting

### Script Can't SSH to Master
```bash
# Test SSH manually
ssh -i ~/.ssh/id_rsa_k8s ajasta@<MASTER_IP> hostname

# Check if Kubernetes is ready
ssh -i ~/.ssh/id_rsa_k8s ajasta@<MASTER_IP> "sudo kubectl get nodes"
```

### Terraform Outputs Missing
```bash
# Generate outputs manually
cd terraform
./scripts/generate-outputs.sh
```

### kubectl Shows Wrong Cluster
```bash
# Check which config is being used
echo $KUBECONFIG

# Use specific config
export KUBECONFIG=~/.kube/config
kubectl get nodes
```

## Best Practices

1. **Run after every Terraform apply**
   ```bash
   terraform apply && cd ../ansible-ci && ./scripts/update-kubeconfig.sh
   ```

2. **Use Ansible playbook for automatic update**
   ```bash
   ansible-playbook -i inventory.ini k8s-bootstrap.yml
   ```

3. **Keep old backups for rollback**
   ```bash
   ls -la ~/.kube/config.backup.*
   ```

4. **Check IP before connecting**
   ```bash
   aj-k8s-info  # or ./scripts/update-kubeconfig.sh --print-ip-only
   ```

## What's Next

1. **Load the shell aliases** (optional but recommended)
   ```bash
   # Add to ~/.zshrc
   source ~/IdeaProjects/petrelevich/ajasta-app/ansible-ci/scripts/shell-aliases.sh
   ```

2. **Run your Terraform apply**
   ```bash
   cd terraform
   terraform apply
   ```

3. **Bootstrap Kubernetes**
   ```bash
   cd ../ansible-ci
   ansible-playbook -i inventory.ini k8s-bootstrap.yml
   # Kubeconfig is updated automatically!
   ```

4. **Use kubectl immediately**
   ```bash
   kubectl get nodes
   kubectl get pods -A
   ```

---

**Your kubeconfig will always stay in sync with your Terraform infrastructure!** 🚀

No more manually updating IPs or wondering why kubectl can't connect!

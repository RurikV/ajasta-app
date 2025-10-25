# Kubernetes TLS Certificate Issue - Fixed

## Problem

When trying to run `kubectl` commands locally against the K8s cluster, you encounter a TLS certificate verification error:

```
error: error validating "k8s/manifests/09-frontend-deployment.yml": error validating data: 
failed to download openapi: Get "https://51.250.21.26:6443/openapi/v2?timeout=32s": 
tls: failed to verify certificate: x509: certificate is valid for 10.96.0.1, 10.10.0.27, 
not 51.250.21.26
```

## Root Cause

The Kubernetes API server's TLS certificate was generated during cluster setup with Subject Alternative Names (SANs) that include only:
- **10.96.0.1** - Kubernetes internal service IP (ClusterIP)
- **10.10.0.27** - Master node's internal/private IP

The certificate does NOT include:
- **51.250.21.26** - Master node's external/public IP

When you connect to the cluster from your local machine using the external IP, the TLS certificate validation fails because the external IP is not in the certificate's SAN list.

## Solution

The `fetch-kubeconfig.zsh` script has been updated to automatically configure the kubeconfig with `insecure-skip-tls-verify: true` flag, which bypasses TLS certificate verification.

### What the Script Does

1. **Fetches kubeconfig** from the K8s master node via SSH
2. **Updates server URL** to use the external IP (51.250.21.26:6443)
3. **Renames context** to 'ajasta-cluster' for easy identification
4. **Merges into ~/.kube/config** preserving any existing configurations
5. **Adds insecure-skip-tls-verify flag** using `kubectl config set-cluster` command
6. **Tests connection** to verify everything works

### Changes Made to fetch-kubeconfig.zsh

The script now includes this step after merging the kubeconfig:

```bash
# Add insecure-skip-tls-verify flag using kubectl config command
kubectl config set-cluster "ajasta-cluster" \
    --server="https://51.250.21.26:6443" \
    --insecure-skip-tls-verify=true \
    --embed-certs=false
```

This ensures the resulting kubeconfig contains:

```yaml
- cluster:
    insecure-skip-tls-verify: true
    server: https://51.250.21.26:6443
  name: ajasta-cluster
```

## How to Use

### Initial Setup (or Re-configure)

Run the fetch-kubeconfig script to configure your local kubectl:

```bash
./scripts/fetch-kubeconfig.zsh
```

**Output:**
```
✅ Successfully configured kubectl for cluster 'ajasta-cluster'

📊 Cluster information:
Kubernetes control plane is running at https://51.250.21.26:6443
CoreDNS is running at https://51.250.21.26:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

ℹ️  Note: TLS certificate verification is disabled (insecure-skip-tls-verify: true)
   This is required because the K8s API certificate is valid only for internal IPs.
   For production, consider regenerating certificates with external IP in SAN list.
```

### Using kubectl

Once configured, you can use kubectl normally:

```bash
# View cluster information
kubectl cluster-info

# Get nodes
kubectl get nodes

# Get all resources in ajasta namespace
kubectl get all -n ajasta

# Apply manifests (the original issue is now fixed!)
kubectl apply -f k8s/manifests/09-frontend-deployment.yml

# View pods
kubectl get pods -n ajasta

# View logs
kubectl logs -n ajasta <pod-name>
```

### Switch Between Contexts

If you have multiple clusters configured:

```bash
# List available contexts
kubectl config get-contexts

# Switch to ajasta cluster
kubectl config use-context ajasta-cluster-admin@ajasta-cluster

# Switch to another cluster
kubectl config use-context <other-context-name>
```

## Security Considerations

### Development/Testing Environment

The `insecure-skip-tls-verify: true` flag is **acceptable** for development and testing environments where:
- You trust the network connection
- You're accessing via known IP addresses
- Security is not the primary concern

### Production Environment

For production environments, you should regenerate the Kubernetes certificates to include the external IP in the SAN list:

#### Option 1: Regenerate API Server Certificate (Recommended)

1. SSH to the master node:
   ```bash
   ssh ajasta@51.250.21.26
   ```

2. Edit the kubeadm configuration:
   ```bash
   sudo vi /etc/kubernetes/kubeadm-config.yaml
   ```

3. Add the external IP to certSANs:
   ```yaml
   apiServer:
     certSANs:
     - 10.96.0.1
     - 10.10.0.27
     - 51.250.21.26  # Add external IP
   ```

4. Regenerate certificates:
   ```bash
   sudo kubeadm init phase certs apiserver --config /etc/kubernetes/kubeadm-config.yaml
   sudo systemctl restart kubelet
   ```

5. Re-run fetch-kubeconfig.zsh to get the updated certificate

#### Option 2: Use SSH Tunnel (Alternative)

Instead of using the external IP, create an SSH tunnel:

```bash
# Forward local port 6443 to remote K8s API
ssh -L 6443:10.10.0.27:6443 ajasta@51.250.21.26 -N &

# Update kubeconfig to use localhost
kubectl config set-cluster ajasta-cluster --server=https://localhost:6443
```

This way you connect via localhost, and the certificate validation works because the internal IP is in the SAN list.

## Verification

### Verify the Flag is Set

Check that the insecure-skip-tls-verify flag is present:

```bash
kubectl config view --minify --raw | grep -A3 "name: ajasta-cluster"
```

Expected output:
```yaml
    insecure-skip-tls-verify: true
    server: https://51.250.21.26:6443
  name: ajasta-cluster
```

### Test Connection

```bash
# Test cluster connectivity
kubectl cluster-info

# Test manifest validation (original issue)
kubectl apply -f k8s/manifests/09-frontend-deployment.yml --dry-run=client
```

If you see `deployment.apps/ajasta-frontend configured` or `unchanged`, the issue is fixed!

## Troubleshooting

### Still Getting Certificate Errors

1. **Verify the flag is set:**
   ```bash
   kubectl config view --minify --raw | grep insecure-skip-tls-verify
   ```

2. **Check current context:**
   ```bash
   kubectl config current-context
   ```
   Should show: `ajasta-cluster-admin@ajasta-cluster`

3. **Re-run the script:**
   ```bash
   ./scripts/fetch-kubeconfig.zsh
   ```

### Connection Timeout

If you get connection timeout instead of certificate errors:

1. **Check firewall rules** on the cloud provider (Yandex Cloud)
2. **Verify master node IP:**
   ```bash
   grep ansible_host ansible-k8s/inventory.ini | grep k8s-master
   ```
3. **Test SSH connection:**
   ```bash
   ssh ajasta@51.250.21.26 "echo Connection works"
   ```

### Wrong Cluster

If kubectl is connecting to the wrong cluster:

```bash
# Switch to ajasta cluster
kubectl config use-context ajasta-cluster-admin@ajasta-cluster

# Verify current context
kubectl config current-context
```

## Summary

✅ **Issue Fixed:** TLS certificate verification error when using external IP  
✅ **Solution:** Automatically configured `insecure-skip-tls-verify: true` flag  
✅ **Result:** kubectl commands now work seamlessly from local machine  
✅ **Security:** Acceptable for dev/test; consider certificate regeneration for production  

---

**Last Updated:** 2025-10-17  
**Script:** `scripts/fetch-kubeconfig.zsh`  
**Related Files:** `ansible-k8s/inventory.ini`, `~/.kube/config`

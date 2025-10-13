# Kubernetes Resume/Recovery Playbook

## Overview

The `k8s-resume.yml` playbook is designed to recover from Kubernetes API server startup failures without re-running expensive infrastructure provisioning steps (VM creation, network setup, etc.).

## When to Use

Use this playbook when:
- ✅ VMs are already created and running (k8s-master, k8s-worker-1, k8s-worker-2)
- ✅ `kubeadm init` has been executed but API server is not responding
- ✅ You want to diagnose and fix the API server issue without destroying infrastructure
- ✅ The main `k8s-provision.yml` playbook failed at the "Wait for API server to respond" task

**Do NOT use this playbook if:**
- ❌ VMs don't exist yet (use `k8s-provision.yml` instead)
- ❌ You want to start from scratch (use `k8s-provision.yml` with fresh VMs)
- ❌ The cluster is already working (no need for recovery)

## What This Playbook Does

### 1. Status Check
- Verifies if admin.conf exists (cluster initialized)
- Tests if API server is already responding
- Shows current cluster status

### 2. Enhanced Diagnostics (if API server not responding)
- **Container status**: Shows kube-apiserver container state
- **Detailed logs**: Last 300 lines of API server logs
- **Certificate validation**: 
  - Checks API server certificate validity and expiration
  - Verifies CA certificate
  - Confirms API server key exists
- **Connectivity tests**:
  - Port 6443 listening check
  - TCP connection test
  - HTTPS connection test (with certificate validation bypassed)
- **etcd connectivity**:
  - Port 2379 listening check
  - etcd health endpoint test
- **Error pattern analysis**:
  - Certificate/TLS errors
  - etcd connection errors
  - OOM/resource errors
  - Fatal/panic errors

### 3. Recovery Actions
- Restarts kubelet service to recreate API server container
- Waits for container to stabilize (15 seconds)
- Checks if API server container is running
- Waits for API server to become ready (up to 2 minutes)

### 4. Finalization
- Installs CNI (Flannel) if not already installed
- Shows cluster nodes status
- Provides next steps for worker node joining

## Prerequisites

### 1. Inventory File Must Be Populated

You **must** have a valid `inventory.ini` with the k8s_master host defined. The VMs must already exist.

**Option A: Auto-generate inventory** (if VMs exist in Yandex Cloud):
```bash
cd ansible-k8s
./generate-inventory.sh
```

**Option B: Manual inventory** (copy from example):
```bash
cd ansible-k8s
cp inventory.ini.example inventory.ini
# Edit inventory.ini and replace:
#   <MASTER_PUBLIC_IP> with actual master public IP
#   <WORKER1_INTERNAL_IP> with actual worker1 internal IP
#   <WORKER2_INTERNAL_IP> with actual worker2 internal IP
```

Example inventory.ini:
```ini
[local]
localhost ansible_connection=local

[k8s_master]
k8s-master ansible_host=158.160.31.61 ansible_user=ajasta ansible_become=true

[k8s_workers]
k8s-worker-1 ansible_host=10.10.0.5 ansible_user=ajasta ansible_become=true ansible_ssh_common_args='-o ProxyJump=ajasta@158.160.31.61 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
k8s-worker-2 ansible_host=10.10.0.6 ansible_user=ajasta ansible_become=true ansible_ssh_common_args='-o ProxyJump=ajasta@158.160.31.61 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'

[k8s:children]
k8s_master
k8s_workers

[k8s:vars]
ansible_ssh_private_key_file=~/.ssh/id_rsa
ansible_ssh_common_args=-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null -o CheckHostIP=no -o ConnectTimeout=10 -o ServerAliveInterval=5 -o ServerAliveCountMax=3 -o ConnectionAttempts=10
```

### 2. SSH Access Must Be Working

Test SSH connectivity before running the playbook:
```bash
# Test master (direct connection)
ssh ajasta@<MASTER_PUBLIC_IP>

# Test workers (via ProxyJump)
ssh -J ajasta@<MASTER_PUBLIC_IP> ajasta@<WORKER1_INTERNAL_IP>
ssh -J ajasta@<MASTER_PUBLIC_IP> ajasta@<WORKER2_INTERNAL_IP>
```

If SSH doesn't work, check:
- SSH keys are correct (~/.ssh/id_rsa exists and has correct permissions)
- VMs are running: `yc compute instance list`
- Security groups allow SSH (port 22)

### 3. VMs Must Already Exist

Verify VMs exist:
```bash
yc compute instance list
```

You should see k8s-master, k8s-worker-1, and k8s-worker-2 in RUNNING state.

## Usage

### Basic Usage
```bash
cd /path/to/ajasta-app
ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-resume.yml
```

### With Verbose Output
```bash
ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-resume.yml -v
```

### Very Verbose (for debugging)
```bash
ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-resume.yml -vvv
```

## Expected Output

### If API Server is Already Working
```
TASK [Cluster status summary] **************
ok: [k8s-master] => {
    "msg": [
        "Admin config exists: True",
        "API server responding: True"
    ]
}

TASK [Success message] *********************
ok: [k8s-master] => {
    "msg": "✓ API server is responding. Cluster is ready for CNI installation and worker join."
}
```

### If API Server Needs Recovery
The playbook will:
1. Show detailed diagnostics (certificates, connectivity, logs)
2. Attempt to restart kubelet
3. Wait for API server to become ready
4. Install CNI if needed
5. Show cluster status

### If Recovery Fails
```
TASK [Fail if API server is still not working] ***
fatal: [k8s-master]: FAILED! => {
    "msg": "API server is still not responding after diagnostics and recovery attempts.\n\nReview the diagnostic output above for specific issues:\n- Certificate validity and expiration\n- etcd connectivity\n- Container crash loops\n- Resource constraints (OOM)\n\nManual recovery may be required..."
}
```

## Troubleshooting

### Common Issues and Solutions

#### 1. API Server Container Crash Loop

**Symptoms**: Container repeatedly restarts, logs show errors

**Common causes**:
- **Certificate errors**: Check certificate validity
- **etcd connection**: Verify etcd is healthy
- **Resource constraints**: Check memory/disk space

**Solution**:
```bash
# SSH to master
ssh ajasta@<MASTER_PUBLIC_IP>

# Check container logs
sudo crictl ps -a | grep apiserver
sudo crictl logs <container-id> | tail -50

# If certificates expired, regenerate
sudo kubeadm certs renew all

# If etcd issues, check etcd logs
sudo crictl logs $(sudo crictl ps -a | grep etcd | awk '{print $1}')
```

#### 2. Insufficient Resources (OOM)

**Symptoms**: Logs show "OOMKilled" or memory errors

**Solution**: Increase VM memory (see `KUBERNETES_API_SERVER_FIX.md`)
```bash
# Edit group_vars/all.yml
master_vm_memory: 4  # Increase from 2GB to 4GB

# Recreate VMs with more memory
ansible-playbook -i inventory.ini k8s-provision.yml
```

#### 3. Certificate Issues

**Symptoms**: Logs show "x509", "certificate", "TLS" errors

**Solution**:
```bash
# SSH to master
ssh ajasta@<MASTER_PUBLIC_IP>

# Check certificate validity
sudo openssl x509 -in /etc/kubernetes/pki/apiserver.crt -noout -dates

# If expired, renew
sudo kubeadm certs renew all
sudo systemctl restart kubelet
```

#### 4. etcd Not Responding

**Symptoms**: API server logs show "context deadline exceeded", "etcdserver" errors

**Solution**:
```bash
# SSH to master
ssh ajasta@<MASTER_PUBLIC_IP>

# Check etcd health
curl http://127.0.0.1:2379/health

# Check etcd container
sudo crictl ps -a | grep etcd
sudo crictl logs <etcd-container-id>

# If etcd is down, may need full reset
sudo kubeadm reset -f
# Then re-run k8s-provision.yml
```

#### 5. Network Connectivity Issues

**Symptoms**: Port 6443 not listening, TCP connection fails

**Solution**:
```bash
# Check if port is listening
sudo ss -tlnp | grep 6443

# Check firewall rules
sudo iptables -L -n | grep 6443

# Verify API server process
sudo ps aux | grep kube-apiserver

# Check kubelet logs
sudo journalctl -u kubelet -n 100 | grep -i error
```

### Manual Reset and Re-provision

If all recovery attempts fail, you may need to reset and re-run full provisioning:

```bash
# SSH to master
ssh ajasta@<MASTER_PUBLIC_IP>

# Perform clean reset
sudo kubeadm reset -f
sudo rm -rf /etc/kubernetes/
sudo rm -rf /var/lib/kubelet/
sudo rm -rf /etc/cni/net.d/
sudo systemctl restart containerd
sudo systemctl restart kubelet

# Exit and re-run full provisioning
exit
ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-provision.yml
```

## Comparison: Resume vs Full Provisioning

| Feature | k8s-resume.yml | k8s-provision.yml |
|---------|----------------|-------------------|
| **Purpose** | Fix API server issues | Full cluster setup |
| **VM Creation** | ❌ Skipped (assumes VMs exist) | ✅ Creates VMs |
| **Network Setup** | ❌ Skipped | ✅ Creates networks |
| **kubeadm init** | ❌ Skipped (assumes done) | ✅ Runs kubeadm init |
| **Diagnostics** | ✅ Extensive diagnostics | ⚠️ Basic diagnostics |
| **Recovery Actions** | ✅ Restarts kubelet, container | ❌ None |
| **CNI Install** | ✅ Yes | ✅ Yes |
| **Worker Join** | ⚠️ Manual (shows command) | ✅ Automatic |
| **Time** | ~5-10 minutes | ~20-30 minutes |
| **Cost** | $0 (uses existing VMs) | $$ (creates new VMs) |
| **Use When** | API server failure only | Fresh start or major issues |

## Next Steps After Successful Resume

Once the resume playbook completes successfully:

### 1. Verify Cluster Health
```bash
# From your local machine
ssh ajasta@<MASTER_PUBLIC_IP> "sudo kubectl get nodes"
ssh ajasta@<MASTER_PUBLIC_IP> "sudo kubectl get pods -A"
```

### 2. Join Worker Nodes (if needed)

Generate join command:
```bash
ssh ajasta@<MASTER_PUBLIC_IP> "sudo kubeadm token create --print-join-command"
```

Copy the output and run on workers:
```bash
# Worker 1
ssh -J ajasta@<MASTER_PUBLIC_IP> ajasta@<WORKER1_INTERNAL_IP> "sudo <join-command>"

# Worker 2
ssh -J ajasta@<MASTER_PUBLIC_IP> ajasta@<WORKER2_INTERNAL_IP> "sudo <join-command>"
```

Or create a playbook to automate worker joining (if needed).

### 3. Verify All Nodes are Ready
```bash
ssh ajasta@<MASTER_PUBLIC_IP> "sudo kubectl get nodes"
```

Should show all nodes in Ready state:
```
NAME            STATUS   ROLES           AGE   VERSION
k8s-master      Ready    control-plane   10m   v1.30.x
k8s-worker-1    Ready    <none>          5m    v1.30.x
k8s-worker-2    Ready    <none>          5m    v1.30.x
```

### 4. Deploy Applications
Your cluster is now ready for application deployments!

## Files

- `k8s-resume.yml` - The resume/recovery playbook
- `K8S_RESUME_PLAYBOOK.md` - This documentation
- `inventory.ini` - Ansible inventory (must be populated)
- `inventory.ini.example` - Template inventory
- `generate-inventory.sh` - Script to auto-generate inventory from Yandex Cloud
- `INVENTORY_SETUP.md` - Detailed inventory setup guide
- `KUBERNETES_API_SERVER_FIX.md` - Root cause analysis and VM memory fix

## Summary

The resume playbook provides:
- ✅ **Fast recovery** - Skip expensive VM provisioning
- ✅ **Detailed diagnostics** - Identify root cause quickly
- ✅ **Automatic recovery** - Restart services and wait for readiness
- ✅ **Cost savings** - Reuse existing infrastructure
- ✅ **Clear guidance** - Know when manual intervention is needed

Use it whenever `k8s-provision.yml` fails at API server startup but the infrastructure is already in place.

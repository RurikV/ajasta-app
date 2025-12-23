# Ansible Inventory Setup for Kubernetes Cluster

## Problem

When running `other-k8s-playbook.yml` directly, you may encounter this error:

```
Failed to connect to the host via ssh: Connection closed by UNKNOWN port 65535
fatal: [k8s-worker-1]: UNREACHABLE!
fatal: [k8s-worker-2]: UNREACHABLE!
```

## Root Cause

The `other-k8s-playbook.yml` playbook expects hosts `k8s-master`, `k8s-worker-1`, and `k8s-worker-2` to be defined in the Ansible inventory. However, the default `inventory.ini` only contains `localhost`.

The worker nodes (k8s-worker-1 and k8s-worker-2) do **not** have public IP addresses - they only have internal IPs and can only be reached via SSH ProxyJump through the master node.

## Infrastructure Overview

```
┌─────────────────┐
│   k8s-master    │  <- Has public IP (e.g., 158.160.31.61)
│  (Public IP)    │
└────────┬────────┘
         │ SSH ProxyJump
         │
    ┌────┴────────────────┐
    │                     │
┌───▼────────┐   ┌───────▼──┐
│k8s-worker-1│   │k8s-worker-2│  <- Internal IPs only
│(Internal IP│   │(Internal IP)│     (e.g., 10.10.0.x)
└────────────┘   └────────────┘
```

## Solutions

### Solution 1: Run the Full Provisioning Playbook (Recommended)

The `k8s-provision.yml` playbook automatically creates a dynamic inventory with the correct ProxyJump configuration:

```bash
ansible-playbook -i inventory.ini k8s-provision.yml
```

This playbook:
1. Provisions VMs in Yandex Cloud
2. Fetches their IP addresses
3. Dynamically adds hosts to inventory with proper SSH ProxyJump settings
4. Runs the other-k8s roles if `use_other_k8s: true` is set

### Solution 2: Auto-Generate Inventory from Yandex Cloud

Use the provided script to fetch current VM IPs and generate a proper inventory:

```bash
./generate-inventory.sh
```

This will create `inventory.ini` with the correct configuration. Then run:

```bash
ansible-playbook -i inventory.ini other-k8s-playbook.yml
```

**Requirements:**
- `yc` CLI tool must be configured (`yc init`)
- `jq` must be installed (`brew install jq` on macOS)
- VMs must already exist in Yandex Cloud

### Solution 3: Manual Inventory Configuration

If you prefer to manually configure the inventory:

1. Get the VM IPs:
   ```bash
   yc compute instance list
   ```

2. Copy the example inventory:
   ```bash
   cp inventory.ini.example inventory.ini
   ```

3. Edit `inventory.ini` and replace placeholders:
   - `<MASTER_PUBLIC_IP>` - Public IP of k8s-master
   - `<WORKER1_INTERNAL_IP>` - Internal IP of k8s-worker-1
   - `<WORKER2_INTERNAL_IP>` - Internal IP of k8s-worker-2

Example:
```ini
[k8s_master]
k8s-master ansible_host=158.160.31.61 ansible_user=ajasta ansible_become=true

[k8s_workers]
k8s-worker-1 ansible_host=10.10.0.5 ansible_user=ajasta ansible_become=true ansible_ssh_common_args='-o ProxyJump=ajasta@158.160.31.61 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
k8s-worker-2 ansible_host=10.10.0.6 ansible_user=ajasta ansible_become=true ansible_ssh_common_args='-o ProxyJump=ajasta@158.160.31.61 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
```

4. Run the playbook:
   ```bash
   ansible-playbook -i inventory.ini other-k8s-playbook.yml
   ```

## Understanding ProxyJump

The `-o ProxyJump=ajasta@<MASTER_IP>` SSH option tells Ansible to:
1. First SSH to the master node (which has a public IP)
2. Then SSH from the master to the worker (using its internal IP)

This is equivalent to manually running:
```bash
ssh -J ajasta@158.160.31.61 ajasta@10.10.0.5
```

## Troubleshooting

### "UNKNOWN port 65535" Error
This error occurs when Ansible cannot resolve or connect to a host. It typically means:
- The host is not defined in inventory
- The host has no public IP and no ProxyJump is configured
- SSH cannot establish the connection

### Verify SSH Connectivity

Test direct connection to master:
```bash
ssh ajasta@<MASTER_PUBLIC_IP>
```

Test ProxyJump to worker:
```bash
ssh -J ajasta@<MASTER_PUBLIC_IP> ajasta@<WORKER_INTERNAL_IP>
```

### Check Ansible Inventory

Verify that Ansible can see your hosts:
```bash
ansible-inventory -i inventory.ini --list
ansible-inventory -i inventory.ini --graph
```

Test connectivity:
```bash
ansible -i inventory.ini k8s -m ping
```

## Files

- `inventory.ini` - Active inventory file (auto-generated or manually created)
- `inventory.ini.example` - Template inventory with placeholders
- `generate-inventory.sh` - Script to auto-generate inventory from Yandex Cloud
- `k8s-provision.yml` - Full provisioning playbook (creates dynamic inventory)
- `other-k8s-playbook.yml` - Kubernetes configuration playbook (requires inventory)

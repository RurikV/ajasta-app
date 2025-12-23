# Kubernetes Worker Node Disk Space Management

## Critical Issue: Disk Pressure on Worker Nodes

This document explains how to identify, prevent, and resolve disk space exhaustion on Kubernetes worker nodes.

## Understanding the Problem

### Symptoms
- Pods with status `Evicted` due to "low on resource: ephemeral-storage"
- Pods stuck in `Pending` state with message: "node(s) had untolerated taint {node.kubernetes.io/disk-pressure: }"
- Container errors: "no space left on device"
- Node conditions showing `DiskPressure=True`

### Root Causes
1. **Container Images**: Docker/containerd stores images and layers that accumulate over time
2. **Container Logs**: Application logs can grow indefinitely if not rotated
3. **Writable Container Layers**: Temporary data written by containers
4. **Build Caches**: leftover build artifacts
5. **Evicted Pods**: Kubernetes doesn't automatically clean up evicted pod containers

## Pre-Deployment Disk Space Check

The deployment playbook now includes automatic disk space checks:

```bash
ansible-playbook deploy-ajasta.yml -i inventory.ini
```

The playbook will:
1. ✅ Check worker nodes for `DiskPressure` condition
2. 📊 Display available ephemeral-storage on each worker
3. ⛔ Fail deployment if disk pressure is detected
4. 📝 Provide cleanup instructions

## Manual Disk Space Checking

### Check Node Disk Pressure Status
```bash
kubectl get nodes -o json | \
  jq -r '.items[] | .metadata.name + " - DiskPressure: " + 
  (.status.conditions[] | select(.type == "DiskPressure") | .status)'
```

### Check Allocatable Storage
```bash
kubectl describe nodes | grep -A 5 "Allocatable"
```

### SSH to Worker and Check Disk Usage
```bash
# SSH to worker node
ssh ajasta@<worker-ip> -J ajasta@<master-ip>

# Check disk usage
df -h

# Check what's using space
sudo du -sh /var/lib/containerd/*
sudo du -sh /var/lib/docker/* 2>/dev/null || echo "Docker not found"
```

## Immediate Cleanup (When Disk Pressure Occurs)

### On Each Worker Node

**IMPORTANT**: Always do this on worker nodes, NOT on the master/control-plane node!

```bash
# SSH to worker node
ssh ajasta@<worker-node-ip> -J ajasta@<master-ip>

# 1. Remove unused container images (containerd)
sudo crictl rmi --prune

# 2. Remove unused Docker images (if Docker is installed)
sudo docker system prune -af

# 3. Clean up stopped containers and build cache
sudo crictl rmc -a 2>/dev/null || true

# 4. Remove old logs (be careful!)
sudo journalctl --vacuum-time=3d
sudo find /var/log -type f -name "*.log" -mtime +7 -delete

# 5. Check remaining space
df -h
```

### From Master Node (Using kubectl)

```bash
# Delete evicted pods (they take up space)
kubectl get pods --all-namespaces -o json | \
  jq -r '.items[] | select(.status.reason == "Evicted") | 
  .metadata.namespace + " " + .metadata.name' | \
  xargs -n 2 kubectl delete pod -n

# Scale down deployments temporarily
kubectl scale deployment ajasta-backend -n ajasta --replicas=0
kubectl scale deployment ajasta-frontend -n ajasta --replicas=0

# Wait a few minutes, then scale back up
kubectl scale deployment ajasta-backend -n ajasta --replicas=1
kubectl scale deployment ajasta-frontend -n ajasta --replicas=1
```

## Prevention Strategies

### 1. Resource Limits (Already Implemented)

The backend deployment now includes ephemeral-storage limits:

```yaml
resources:
  requests:
    ephemeral-storage: "1Gi"
  limits:
    ephemeral-storage: "2Gi"
```

This prevents any single pod from consuming all disk space.

### 2. Reduced Replica Count

Backend replicas reduced from 2 to 1 to conserve disk space:

```yaml
spec:
  replicas: 1  # Previously 2
```

For production with adequate disk space, increase to 2-3 replicas.

### 3. Regular Cleanup Cron Job

Consider creating a Kubernetes CronJob for automated cleanup:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: cleanup-evicted-pods
  namespace: ajasta
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: cleanup-sa
          restartPolicy: OnFailure
          containers:
          - name: kubectl
            image: bitnami/kubectl:latest
            command:
            - /bin/sh
            - -c
            - |
              kubectl get pods --all-namespaces -o json | \
              jq -r '.items[] | select(.status.reason == "Evicted") | 
              .metadata.namespace + " " + .metadata.name' | \
              xargs -n 2 kubectl delete pod -n
```

### 4. Log Rotation

Configure log rotation on worker nodes:

```bash
# /etc/logrotate.d/containers
/var/log/containers/*.log {
    rotate 7
    daily
    compress
    missingok
    notifempty
    maxsize 100M
}
```

### 5. Increase Worker Node Disk Size

The most reliable long-term solution:

Edit `k8s/group_vars/all.yml`:
```yaml
# Increase worker VM disk size (default is usually 10-20GB)
worker_vm_disk_size: 50  # GB
```

Then recreate VMs:
```bash
ansible-playbook yc-destroy.yml -i inventory.ini
ansible-playbook yc-create.yml -i inventory.ini
```

## Monitoring Disk Space

### Set Up Alerts

Monitor these metrics:
- `kubelet_volume_stats_available_bytes`
- `node_filesystem_avail_bytes`
- DiskPressure node condition

### Regular Checks

Add to your operational runbook:
```bash
# Weekly disk space report
kubectl get nodes -o json | \
  jq -r '.items[] | .metadata.name + " - Available: " + 
  .status.allocatable."ephemeral-storage"'
```

## Disk Space Requirements

### Minimum Recommendations

| Component | Disk Space Needed |
|-----------|-------------------|
| OS + System | 5 GB |
| Kubernetes binaries | 2 GB |
| Container images | 5-10 GB |
| Container logs | 2-5 GB |
| Writable layers | 2-5 GB |
| **Total Recommended** | **20-30 GB** |

### Per Application

| Service | Image Size | Runtime Overhead | Total per Pod |
|---------|------------|------------------|---------------|
| PostgreSQL | 240 MB | 1-2 GB | ~2 GB |
| Backend (Spring Boot) | 160 MB | 1-2 GB | ~2 GB |
| Frontend (nginx) | 50 MB | 100-200 MB | ~300 MB |

With 2 backend + 2 frontend + 1 database = ~6.5 GB minimum

## Troubleshooting

### Pods Keep Getting Evicted

**Cause**: Worker node has less than 10% free disk space (default threshold)

**Solution**:
1. Immediate cleanup (see above)
2. Add ephemeral-storage limits to all deployments
3. Reduce replica counts
4. Increase worker node disk size

### "No space left on device" Errors

**Cause**: Node has completely run out of inodes or disk space

**Solution**:
```bash
# Check inodes
df -i

# If out of inodes (rare):
sudo find /var/lib/containerd -type f | wc -l

# Emergency cleanup
sudo systemctl stop containerd
sudo rm -rf /var/lib/containerd/io.containerd.grpc.v1.cri/containers/*
sudo systemctl start containerd
```

### Cannot SSH to Worker Node

**Cause**: Node is unresponsive due to resource exhaustion

**Solution**:
1. Use Yandex Cloud console to access the VM
2. Reboot the VM from cloud console
3. After reboot, perform cleanup immediately
4. Consider increasing VM resources

## Best Practices

1. ✅ **Monitor disk usage proactively** - Don't wait for DiskPressure
2. ✅ **Set ephemeral-storage limits** - Prevent runaway disk usage
3. ✅ **Regular cleanup** - Schedule weekly/monthly maintenance
4. ✅ **Right-size your workers** - 30GB minimum for production
5. ✅ **Use external logging** - Ship logs to external system (ELK, etc.)
6. ✅ **Image cleanup** - Remove old/unused images regularly
7. ✅ **Test disaster recovery** - Practice cleanup procedures

## Emergency Recovery Procedure

If deployment completely fails due to disk pressure:

```bash
# 1. SSH to EACH worker node and run cleanup
for worker in k8s-worker-1 k8s-worker-2; do
  echo "Cleaning $worker..."
  ssh ajasta@$worker -J ajasta@<master-ip> \
    "sudo crictl rmi --prune && sudo docker system prune -af"
done

# 2. Delete ALL pods in ajasta namespace
kubectl delete pods --all -n ajasta

# 3. Wait for cleanup
sleep 60

# 4. Re-run deployment with reduced replicas
ansible-playbook deploy-ajasta.yml -i inventory.ini
```

## Additional Resources

- [Kubernetes Resource Management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)
- [Node Conditions](https://kubernetes.io/docs/concepts/architecture/nodes/#condition)
- [Ephemeral Storage Management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#local-ephemeral-storage)

## Summary

Disk space management is critical for Kubernetes stability. The deployment now includes:
- ✅ Pre-deployment disk checks
- ✅ Ephemeral-storage resource limits
- ✅ Reduced default replicas
- ✅ Clear error messages and recovery instructions

Always ensure worker nodes have adequate disk space (minimum 20GB, recommended 30-50GB) before deploying applications.

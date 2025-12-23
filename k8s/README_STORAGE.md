# Storage Backend Configuration for Ajasta Kubernetes Deployment

This document explains how to configure and use different storage backends (Longhorn or Rook) for persistent storage in the Ajasta Kubernetes application.

## Overview

The Ajasta application uses persistent storage for PostgreSQL database. You can choose between two distributed storage solutions:

1. **Longhorn** - Lightweight, easy-to-use distributed block storage
2. **Rook** - Enterprise-grade Ceph-based distributed storage with advanced features

## Storage Backend Comparison

| Feature | Longhorn | Rook-Ceph |
|---------|----------|-----------|
| **Complexity** | Simple | Advanced |
| **Resource Usage** | Low | Medium-High |
| **Setup Time** | Fast (2-5 min) | Slower (5-10 min) |
| **Dependencies** | iSCSI (open-iscsi) | RBD kernel module |
| **Best For** | Development, Small clusters | Production, Large clusters |
| **Min Requirements** | 1 worker node | 1+ worker nodes |
| **Storage Features** | Basic replication | Advanced (snapshots, encryption, multi-pool) |

## Choosing a Storage Backend

### Use Longhorn if:
- You want quick and simple setup
- You have limited resources
- You need basic block storage functionality
- You're deploying to development/test environments
- Your cluster has 1-3 worker nodes

### Use Rook if:
- You need enterprise-grade storage features
- You have sufficient resources (4GB+ RAM per worker)
- You want advanced Ceph features (snapshots, cloning, encryption)
- You're deploying to production environments
- Your cluster has multiple worker nodes with dedicated storage devices

## Configuration

### Setting the Storage Backend

Edit `k8s/deploy-ajasta.yml` and set the `storage_backend` variable:

```yaml
vars:
  manifests_dir: "manifests"
  app_namespace: "ajasta"
  # Storage backend to use: "longhorn" or "rook"
  storage_backend: "longhorn"  # or "rook"
```

**Prerequisites:**
- Worker nodes must have `open-iscsi` installed (auto-installed by playbook)
- At least 10GB free disk space on worker nodes
- iSCSI kernel module support

**Installation:**
The playbook will automatically:
1. Install open-iscsi on all worker nodes
2. Configure iSCSI prerequisites
3. Deploy Longhorn v1.5.3
4. Create a StorageClass named "longhorn"
5. Set it as the default StorageClass

**Configuration Options:**

You can customize Longhorn by modifying `k8s/roles/storage_longhorn/defaults/main.yml`:

```yaml
longhorn_namespace: "longhorn-system"
longhorn_version: "v1.5.3"
storage_class_name: "longhorn"
set_as_default: true
```

### Using Rook-Ceph

```yaml
storage_backend: "rook"
```

**Prerequisites:**
- At least 1 worker node with available storage
- 4GB+ RAM per worker node recommended
- RBD kernel module support (auto-loaded)
- For production: Dedicated block devices or additional disks on worker nodes

**Installation:**
The playbook will automatically:
1. Install Rook operator v1.12.9
2. Create a CephCluster with 1 monitor and 1 manager
3. Wait for Ceph OSDs to be created and running
4. Create a CephBlockPool for storage
5. Create a StorageClass named "longhorn" (for compatibility)
6. Wait for Rook CSI driver components to be ready:
   - CSI RBD provisioner deployment
   - CSI RBD plugin DaemonSet on all nodes
   - CSI secrets (rook-csi-rbd-provisioner, rook-csi-rbd-node)
7. Set it as the default StorageClass

**Configuration Options:**

You can customize Rook by modifying `k8s/roles/storage_rook/defaults/main.yml`:

```yaml
rook_namespace: "rook-ceph"
rook_version: "v1.12.9"
storage_class_name: "longhorn"  # For compatibility with existing manifests
set_as_default: true
ceph_mon_count: 1
ceph_mgr_count: 1

# Storage type: "directories" (default, for VMs/cloud) or "devices" (for bare metal)
ceph_storage_type: "directories"

# Directory storage configuration (used when ceph_storage_type is "directories")
ceph_storage_directory: "/var/lib/rook/storage"

# Device storage configuration (used when ceph_storage_type is "devices")
ceph_use_all_nodes: true
ceph_storage_devices: "all"  # or specify: "/dev/sdb,/dev/sdc"
```

**Important Notes for Rook:**
- **Default storage mode**: Directory-based storage for VM/cloud compatibility
- **Directory storage**: Uses `/var/lib/rook/storage` on worker nodes (auto-created)
- **Device storage**: For production with dedicated raw block devices
- Initial cluster creation may take 5-10 minutes
- The playbook automatically creates storage directories on all worker nodes
- Minimum replica count is set to 1 for testing (increase for production)

## Deployment

Deploy the application with your chosen storage backend:

```bash
cd k8s
ansible-playbook -i inventory deploy-ajasta.yml
```

The playbook will:
1. Validate the storage backend parameter
2. Install the selected storage backend
3. Wait for storage to be ready
4. Deploy PostgreSQL with persistent storage
5. Deploy the rest of the application

## Verification

### Check Storage Backend Status

**For Longhorn:**
```bash
# Check Longhorn pods
kubectl get pods -n longhorn-system

# Check Longhorn UI (if exposed)
kubectl get svc -n longhorn-system
```

**For Rook:**
```bash
# Check Rook pods
kubectl get pods -n rook-ceph

# Check Ceph cluster status
kubectl get cephcluster -n rook-ceph

# Check Ceph health (if tools pod is running)
kubectl -n rook-ceph exec -it deploy/rook-ceph-tools -- ceph status
```

### Check StorageClass

```bash
# List all StorageClasses
kubectl get storageclass

# Should show "longhorn" as default
kubectl get storageclass longhorn -o yaml
```

### Check PostgreSQL PVC

```bash
# Check PVC status in ajasta namespace
kubectl get pvc -n ajasta

# Should show "Bound" status
kubectl describe pvc -n ajasta
```

## Troubleshooting

### Longhorn Issues

**Problem: Longhorn pods in CrashLoopBackOff**
- **Cause:** iSCSI not installed or running
- **Solution:** 
  ```bash
  # On each worker node
  sudo systemctl status iscsid
  sudo systemctl start iscsid
  sudo systemctl enable iscsid
  ```

**Problem: StorageClass not created**
- **Cause:** Longhorn manager pods not ready
- **Solution:** Check logs:
  ```bash
  kubectl logs -n longhorn-system -l app=longhorn-manager
  ```

### Rook Issues

**Problem: Ceph OSDs not starting**

The playbook now provides detailed diagnostics when OSDs fail to start. Common causes and solutions:

**For directory-based storage (default):**
- **Cause:** Storage directory doesn't exist or has wrong permissions
- **Solution:** The playbook automatically creates directories, but if it fails:
  ```bash
  # SSH to each worker node and run:
  sudo mkdir -p /var/lib/rook/storage
  sudo chmod 755 /var/lib/rook/storage
  ```

- **Cause:** Insufficient disk space on worker nodes
- **Solution:** Ensure at least 10GB free space:
  ```bash
  df -h /var/lib/rook/storage
  ```

**For device-based storage:**
- **Cause:** No available raw block devices
- **Solution:** 
  ```bash
  # Check available devices
  lsblk
  
  # Devices must be completely empty (no partitions, no filesystem)
  # Wipe a device if needed (WARNING: destroys all data)
  sudo wipefs -a /dev/sdX
  ```

**Diagnostic commands:**
```bash
# Check OSD pods
kubectl get pods -n rook-ceph -l app=rook-ceph-osd

# Check Rook operator logs for OSD creation issues
kubectl logs -n rook-ceph -l app=rook-ceph-operator --tail=50

# Check CephCluster status
kubectl get cephcluster -n rook-ceph -o yaml
```

**Problem: OSD prepare completes with "no devices matched the storage settings"**

This issue occurs when Rook-Ceph's OSD prepare process runs successfully but cannot find any suitable storage (directories or devices) to use.

- **Symptoms:**
  - OSD prepare pods show "Completed" status
  - OSD prepare logs contain: `"skipping OSD configuration as no devices matched the storage settings for this node"`
  - No OSD daemon pods are created (Running OSD Pods: 0)
  - Logs may show: `"skipping device vda1: Insufficient space (<5GB)"` or similar rejection messages
  - Deployment fails with "CRITICAL: Rook-Ceph cluster is not healthy"

- **Root Cause:** The configured storage (directories) don't exist on worker nodes, or existing storage doesn't meet Ceph's requirements:
  1. **Storage directories missing** (most common): `/var/lib/rook/storage` doesn't exist on worker nodes
  2. **Directories not empty**: Ceph requires completely empty directories for new OSDs
  3. **Insufficient space**: Devices/partitions smaller than 5GB are rejected
  4. **Devices in use**: All available storage is mounted or partitioned (like root filesystem)
  5. **Wrong configuration**: Directory-based storage configured but directories not created properly

- **Detection:** The deployment now automatically detects this and displays:
  ```
  **DETECTED: "no devices matched the storage settings" in OSD prepare logs**
  ```

- **Solution:**

  **Step 1: Verify the issue in OSD prepare logs**
  ```bash
  # Check OSD prepare logs
  kubectl logs -n rook-ceph -l app=rook-ceph-osd-prepare --tail=100
  
  # Look for the message:
  # "skipping OSD configuration as no devices matched the storage settings"
  ```

  **Step 2: Verify storage directories exist on ALL worker nodes**
  ```bash
  # SSH to EACH worker node and check:
  sudo ls -ld /var/lib/rook/storage
  
  # If directory doesn't exist (most common cause):
  # drwxr-xr-x 2 root root 4096 ... /var/lib/rook/storage  <- GOOD
  # ls: cannot access '/var/lib/rook/storage': No such file or directory  <- BAD
  ```

  **Step 3: Create storage directories on all worker nodes**
  ```bash
  # SSH to EACH worker node (k8s-worker-1, k8s-worker-2, k8s-worker-3, etc.) and run:
  sudo mkdir -p /var/lib/rook/storage
  sudo chmod 755 /var/lib/rook/storage
  
  # Verify creation:
  sudo ls -ld /var/lib/rook/storage
  # Should show: drwxr-xr-x ... /var/lib/rook/storage
  ```

  **Step 4: Ensure directories are COMPLETELY EMPTY**
  ```bash
  # On each worker node, verify directory is empty:
  sudo ls -la /var/lib/rook/storage
  # Should show only . and .. entries
  
  # If any files exist, remove them:
  sudo rm -rf /var/lib/rook/storage/*
  ```

  **Step 5: Delete CephCluster and redeploy**
  ```bash
  # Delete the existing CephCluster to force recreation
  kubectl delete cephcluster -n rook-ceph rook-ceph
  
  # Wait for cleanup (1 minute)
  sleep 60
  
  # Re-run the deployment playbook
  cd k8s
  ansible-playbook -i inventory.ini deploy-ajasta.yml
  ```

  **Step 6: Verify OSDs are created**
  ```bash
  # Monitor OSD creation (should see pods within 2-3 minutes)
  kubectl get pods -n rook-ceph -l app=rook-ceph-osd -w
  
  # Should see pods transitioning to Running:
  # rook-ceph-osd-0-xxxxx   1/1     Running   0   2m
  # rook-ceph-osd-1-xxxxx   1/1     Running   0   2m
  ```

**Problem: OSD prepare jobs completed but no OSD daemon pods running**

This is the **most common issue** with Rook-Ceph in resource-constrained environments (VMs, small nodes).

- **Symptoms:**
  - Deployment fails with "No Ceph OSD daemon pods are running"
  - You see `rook-ceph-osd-prepare-*` pods in "Completed" status
  - BUT no `rook-ceph-osd-*` pods in "Running" status
  - Error message shows: "Running OSD Pods: 0"
  - Ceph cluster health shows "HEALTH_WARN" or error

- **Cause:** OSD preparation succeeded (storage detected, metadata created), but OSD daemon pods failed to start due to:
  1. **Insufficient resources** (most common): Worker nodes don't have enough CPU/Memory
     - Each OSD requires: 2Gi memory (limit), 500m CPU (request)
     - If worker nodes are small or already heavily loaded, OSDs can't be scheduled
  2. **Directory not empty**: Ceph requires completely empty directories for new OSDs
  3. **Permission issues**: OSD daemon can't access the storage directory

- **Detection:** The deployment now automatically collects and displays OSD prepare logs when this occurs

- **Solution:**

  **Step 1: Verify the issue by checking OSD prepare logs**
  ```bash
  # The deployment already displays these logs in the error message
  # Or check manually:
  kubectl logs -n rook-ceph -l app=rook-ceph-osd-prepare --tail=100
  
  # Look for successful preparation messages like:
  # "OSD prepare completed successfully"
  # "OSD metadata created"
  ```

  **Step 2: Check worker node resources (most common fix needed)**
  ```bash
  # Check allocated resources on worker nodes
  kubectl describe nodes | grep -A 5 "Allocated resources"
  
  # Example output showing high usage:
  # Allocated resources:
  #   cpu:     3500m (87% of 4000m)    <- High CPU usage
  #   memory:  7Gi (87% of 8Gi)        <- High memory usage
  
  # If CPU > 75% or Memory > 75%, you have resource constraints
  ```

  **Step 3: Fix resource constraints (choose one approach)**

  **Option A: Reduce OSD resource requirements (recommended for VMs/small clusters)**
  
  Edit `k8s/roles/storage_rook/tasks/main.yml` and modify the CephCluster resource limits:
  
  Find the OSD resources section (around line 269-282 or 332-345) and reduce:
  ```yaml
  osd:
    limits:
      cpu: "1000m"      # Reduced from 2000m
      memory: "2Gi"     # Reduced from 4Gi
    requests:
      cpu: "250m"       # Reduced from 500m
      memory: "1Gi"     # Reduced from 2Gi
  ```
  
  Then delete the CephCluster and re-run deployment:
  ```bash
  kubectl delete cephcluster -n rook-ceph rook-ceph
  # Wait 1 minute for cleanup
  cd k8s
  ansible-playbook -i inventory deploy-ajasta.yml
  ```

  **Option B: Reduce replica count (for single-node testing)**
  
  The default configuration already uses `size: 1` for replicas, which is minimal.

  **Option C: Add more resources to worker nodes**
  
  If using VMs, increase worker node resources:
  - Increase RAM to at least 6GB per worker
  - Increase CPU to at least 4 cores per worker

  **Step 4: Ensure storage directory is empty**
  ```bash
  # SSH to each worker node
  sudo ls -la /var/lib/rook/storage
  
  # If directory contains files from previous failed attempt:
  sudo rm -rf /var/lib/rook/storage/*
  # Or use a different directory by changing ceph_storage_directory in defaults/main.yml
  ```

  **Step 5: Verify OSD daemon pods start after fixes**
  ```bash
  # Re-run the deployment
  cd k8s
  ansible-playbook -i inventory deploy-ajasta.yml
  
  # Monitor OSD pods
  kubectl get pods -n rook-ceph -l app=rook-ceph-osd -w
  
  # Should see pods transition to Running state within 2-3 minutes
  ```

**Problem: CephBlockPool not becoming ready**
- **Cause:** OSDs not healthy or insufficient OSDs for replication
- **Solution:**
  ```bash
  # Check if OSDs are running
  kubectl get pods -n rook-ceph -l app=rook-ceph-osd
  
  # Check Ceph cluster health
  kubectl get cephcluster -n rook-ceph -o jsonpath='{.status.ceph.health}'
  
  # Should show "HEALTH_OK" or "HEALTH_WARN"
  # If showing errors, check manager logs
  kubectl logs -n rook-ceph -l app=rook-ceph-mgr --tail=50
  ```

**Problem: PVC provisioning fails with "context deadline exceeded"**
- **Cause:** Rook CSI driver not ready or not running properly
- **Solution:**
  ```bash
  # Check CSI provisioner pods
  kubectl get pods -n rook-ceph -l app=csi-rbdplugin-provisioner
  
  # Check CSI plugin DaemonSet (should be running on all worker nodes)
  kubectl get pods -n rook-ceph -l app=csi-rbdplugin
  
  # Check CSI provisioner logs for errors
  kubectl logs -n rook-ceph -l app=csi-rbdplugin-provisioner -c csi-provisioner
  
  # Verify CSI secrets exist
  kubectl get secret -n rook-ceph rook-csi-rbd-provisioner
  kubectl get secret -n rook-ceph rook-csi-rbd-node
  ```
  
  If CSI components are not running:
  1. Wait for CephCluster to be fully healthy first
  2. Check Rook operator logs: `kubectl logs -n rook-ceph -l app=rook-ceph-operator`
  3. The deployment now automatically waits for CSI components to be ready

**Problem: PVC provisioning fails with "operation with the given Volume ID already exists"**
- **Cause:** Previous failed provisioning attempt left partial volume state
- **Solution:**
  ```bash
  # Delete the stuck PVC
  kubectl delete pvc -n ajasta postgres-data-ajasta-postgres-0
  
  # Check if any PVs are stuck in Released state
  kubectl get pv
  
  # Delete any stuck PVs (if exists)
  kubectl delete pv <pv-name>
  
  # Restart the CSI provisioner to clear state
  kubectl rollout restart deployment/csi-rbdplugin-provisioner -n rook-ceph
  
  # Wait for CSI provisioner to be ready
  kubectl wait --for=condition=available deployment/csi-rbdplugin-provisioner -n rook-ceph --timeout=300s
  
  # Redeploy PostgreSQL StatefulSet
  kubectl delete statefulset ajasta-postgres -n ajasta --cascade=orphan
  kubectl apply -f manifests/03-postgres-statefulset.yml
  ```

**Problem: PVC stuck in Pending with continuous "Provisioning" events but no volume created**
- **Symptoms:** 
  - PVC shows "Pending" status for extended time (hours)
  - PVC events show continuous "Provisioning" and "ExternalProvisioning" messages
  - No error messages in PVC events
  - Rook operator and CSI provisioner pods are Running
  - PostgreSQL pod cannot start (waiting for PVC)

- **Cause:** Rook CSI components are operational, but the underlying Ceph cluster is not healthy or CephBlockPool is not ready. This can happen if:
  - Rook was installed in a previous run but Ceph cluster never became fully healthy
  - OSD pods are not running (no storage backend)
  - CephBlockPool is stuck in a non-Ready state
  - Ceph cluster health is in HEALTH_ERR state

- **Detection:** The deployment playbook now automatically checks Ceph cluster health and will fail with detailed diagnostics if:
  - Ceph cluster health is not HEALTH_OK or HEALTH_WARN
  - CephBlockPool "replicapool" is not in Ready or Created state
  - No OSD pods are running

- **Solution:**
  ```bash
  # 1. Check Ceph cluster overall health
  kubectl get cephcluster -n rook-ceph rook-ceph -o jsonpath='{.status.ceph.health}'
  # Should show: HEALTH_OK or HEALTH_WARN (not HEALTH_ERR or empty)
  
  # 2. Check CephBlockPool status
  kubectl get cephblockpool -n rook-ceph replicapool -o jsonpath='{.status.phase}'
  # Should show: Ready or Created
  
  # 3. Check OSD pods
  kubectl get pods -n rook-ceph -l app=rook-ceph-osd
  # Should have at least 1 pod in Running state
  
  # 4. Get detailed CephCluster status
  kubectl get cephcluster -n rook-ceph rook-ceph -o yaml
  ```

  **If Ceph cluster health is HEALTH_ERR or not-found:**
  ```bash
  # Check why cluster is unhealthy
  kubectl logs -n rook-ceph -l app=rook-ceph-operator --tail=100
  
  # Check monitor pods
  kubectl get pods -n rook-ceph -l app=rook-ceph-mon
  
  # If monitors are not running, Ceph cluster cannot function
  # Check monitor logs for errors
  kubectl logs -n rook-ceph -l app=rook-ceph-mon --tail=50
  ```

  **If CephBlockPool is not Ready:**
  ```bash
  # Get BlockPool details
  kubectl get cephblockpool -n rook-ceph replicapool -o yaml
  
  # Common cause: Not enough OSDs for replication
  # Check OSD count (need at least 1)
  kubectl get pods -n rook-ceph -l app=rook-ceph-osd
  ```

  **If OSD pods are missing (most common issue):**
  ```bash
  # Check OSD preparation logs
  kubectl logs -n rook-ceph -l app=rook-ceph-osd-prepare --tail=100
  
  # For directory-based storage, verify directories exist on worker nodes
  # SSH to each worker node:
  ls -la /var/lib/rook/storage
  # If missing: sudo mkdir -p /var/lib/rook/storage && sudo chmod 755 /var/lib/rook/storage
  
  # Check Rook operator logs for OSD creation issues
  kubectl logs -n rook-ceph -l app=rook-ceph-operator --tail=100 | grep -i osd
  ```

  **Recovery procedure:**
  ```bash
  # 1. Delete stuck PVC to allow clean retry
  kubectl delete pvc -n ajasta postgres-data-ajasta-postgres-0
  
  # 2. If OSDs are missing, ensure storage is available on worker nodes
  # (see directory creation above)
  
  # 3. Restart Rook operator to retry OSD creation
  kubectl rollout restart deployment/rook-ceph-operator -n rook-ceph
  
  # 4. Wait for Ceph cluster to become healthy (can take 5-10 minutes)
  kubectl get cephcluster -n rook-ceph rook-ceph -w
  # Watch until status.ceph.health shows HEALTH_OK or HEALTH_WARN
  
  # 5. Verify CephBlockPool is ready
  kubectl get cephblockpool -n rook-ceph replicapool
  # Status should be "Ready"
  
  # 6. Redeploy PostgreSQL
  kubectl delete statefulset ajasta-postgres -n ajasta --cascade=orphan
  kubectl apply -f manifests/03-postgres-statefulset.yml
  
  # 7. Monitor PVC creation
  kubectl get pvc -n ajasta -w
  # Should transition from Pending to Bound within 1-2 minutes
  ```

  **If all else fails (nuclear option):**
  ```bash
  # Complete Rook reinstallation
  kubectl delete namespace rook-ceph
  kubectl delete crd $(kubectl get crd | grep 'rook.io' | awk '{print $1}')
  
  # Re-run deployment playbook
  cd k8s
  ansible-playbook -i inventory deploy-ajasta.yml
  ```

**Problem: Deployment fails with "no OSDs were created" error**
- **Cause:** The playbook detected that no OSDs were created after 10 minutes
- **Solution:** The error message includes detailed diagnostics. Common fixes:
  1. **Check storage configuration:**
     ```bash
     # Verify storage type in defaults
     cat k8s/roles/storage_rook/defaults/main.yml | grep ceph_storage_type
     ```
  2. **For directory storage:** Manually create directories on worker nodes (see above)
  3. **For device storage:** Ensure raw devices are available and clean
  4. **Check worker node resources:**
     ```bash
     kubectl describe nodes | grep -A 5 "Allocated resources"
     ```

**Problem: Ceph cluster not healthy**
- **Cause:** Insufficient resources or configuration issues
- **Solution:** 
  ```bash
  # Check Ceph status
  kubectl -n rook-ceph get cephcluster
  
  # Check all Ceph component pods
  kubectl get pods -n rook-ceph
  
  # Check operator logs
  kubectl logs -n rook-ceph -l app=rook-ceph-operator --tail=100
  ```

### General Storage Issues

**Problem: PVC stuck in Pending**
- **Cause:** StorageClass not available or provisioner not working
- **Solution:**
  ```bash
  # Check StorageClass
  kubectl get storageclass
  
  # Check PVC events
  kubectl describe pvc -n ajasta
  
  # Check storage backend pods
  kubectl get pods -n longhorn-system  # or -n rook-ceph
  ```

## Switching Storage Backends

**Warning:** Switching storage backends will result in data loss unless you migrate the data first.

To switch from one backend to another:

1. Backup your PostgreSQL data (if needed)
2. Delete the application:
   ```bash
   kubectl delete namespace ajasta
   ```
3. Delete the old storage backend:
   ```bash
   # For Longhorn
   kubectl delete namespace longhorn-system
   
   # For Rook
   kubectl delete namespace rook-ceph
   kubectl delete crd $(kubectl get crd | grep 'rook.io' | awk '{print $1}')
   ```
4. Update `storage_backend` in `deploy-ajasta.yml`
5. Re-run the deployment playbook

## Advanced Configuration

### Rook: Switching Between Storage Types

**Using Directory-Based Storage (Default for VMs/Cloud):**

Edit `k8s/roles/storage_rook/defaults/main.yml`:

```yaml
# Storage type: "directories" for VMs/cloud without dedicated disks
ceph_storage_type: "directories"
ceph_storage_directory: "/var/lib/rook/storage"
```

**Benefits:**
- Works in any environment (VMs, cloud, bare metal)
- No need for dedicated raw block devices
- Automatic directory creation on worker nodes
- Quick setup for development/testing

**Using Device-Based Storage (For Production with Dedicated Disks):**

Edit `k8s/roles/storage_rook/defaults/main.yml`:

```yaml
# Storage type: "devices" for bare metal with dedicated storage disks
ceph_storage_type: "devices"
ceph_use_all_nodes: true
ceph_storage_devices: "all"  # or specify: "/dev/sdb,/dev/sdc"
```

**Benefits:**
- Better performance with dedicated hardware
- Isolated storage from OS disk
- Recommended for production environments

**Requirements:**
- Raw block devices available on worker nodes (e.g., /dev/sdb, /dev/sdc)
- Devices must be completely empty (no partitions, no filesystem)
- Wipe devices before use: `sudo wipefs -a /dev/sdX`

### Rook: Increasing Replication for Production

For production environments with multiple worker nodes, increase replication for data safety.

Edit `k8s/roles/storage_rook/tasks/main.yml` in the CephBlockPool creation section (around line 377):

```yaml
spec:
  failureDomain: host
  replicated:
    size: 3  # Change from 1 to 3 for production
    requireSafeReplicaSize: true  # Change from false to true
```

**Notes:**
- Requires at least 3 worker nodes with OSDs
- Provides data redundancy and high availability
- Increases storage overhead (3x for 3 replicas)

### Longhorn: Custom Storage Path

Edit `k8s/roles/storage_longhorn/tasks/main.yml` and modify the `/var/lib/longhorn` directory path in the prerequisites check task.

## Resources

### Longhorn
- Official Documentation: https://longhorn.io/docs/
- GitHub: https://github.com/longhorn/longhorn
- System Requirements: https://longhorn.io/docs/1.5.3/deploy/install/#installation-requirements

### Rook
- Official Documentation: https://rook.io/docs/rook/latest/
- GitHub: https://github.com/rook/rook
- Ceph Documentation: https://docs.ceph.com/

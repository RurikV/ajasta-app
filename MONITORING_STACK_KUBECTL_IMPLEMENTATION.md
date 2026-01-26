# Grafana + Loki + Promtail + cAdvisor Monitoring Stack (kubectl manifests)

## Overview

This document describes the complete implementation of a production-ready monitoring and logging stack using **kubectl manifests** (no Helm required) for the Ajasta application.

**Components Deployed:**
- ✅ **Loki** (2.9.10) - Log aggregation system
- ✅ **Promtail** (2.9.10) - Log collection agent (DaemonSet)
- ✅ **Grafana** - Visualization (existing from kube-prometheus-stack)
- ✅ **cAdvisor** - Container metrics (DaemonSet)
- ✅ **node_exporter** - System metrics (already deployed with kube-prometheus)
- ✅ **kube-state-metrics** - Kubernetes object metrics (already deployed with kube-prometheus)

**Key Features:**
- No Helm dependency - uses kubectl manifests only
- Monolithic Loki mode (no timeout issues)
- Integrates with existing Grafana from kube-prometheus-stack
- Automatic log collection from all pods
- Full rollback capability with scripts
- GitLab CI/CD integration

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Kubernetes Cluster                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐      ┌──────────────┐                     │
│  │ Applications │ ───→ │  Promtail    │ (DaemonSet)         │
│  │  (pods/logs) │      └──────┬───────┘                     │
│  └──────────────┘             │                             │
│                               ↓                             │
│  ┌─────────────────────────────────────────┐                │
│  │              Loki                       │                │
│  │        (Monolithic Mode)                │                │
│  │        -target=all                      │                │
│  └────────────┬────────────────────────────┘                │
│               │                                             │
│               ↓                                             │
│  ┌─────────────────────────────────────────┐                │
│  │            Grafana                      │                │
│  │     (Existing kube-prometheus)          │                │
│  └─────────────────────────────────────────┘                │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │node_exporter │  │  cAdvisor    │  │kube-state    │       │
│  │ (DaemonSet)  │  │ (DaemonSet)  │  │  (metrics)   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Deployment Files

### Ansible Playbooks

1. **`ansible/k8s/23-deploy-loki-kubectl.yml`**
   - Deploys Loki using kubectl manifests
   - ConfigMap with Loki configuration
   - Deployment with persistent storage (PVC)
   - Service (ClusterIP)
   - Rollback script: `/tmp/rollback-loki-kubectl.sh`

2. **`ansible/k8s/24-deploy-promtail-kubectl.yml`**
   - Deploys Promtail as DaemonSet
   - ConfigMap with Promtail configuration
   - ServiceAccount, ClusterRole, ClusterRoleBinding (RBAC)
   - Rollback script: `/tmp/rollback-promtail-kubectl.sh`

3. **`ansible/k8s/25-configure-grafana-loki.yml`**
   - Deploys cAdvisor DaemonSet for container metrics
   - Configures Grafana Loki datasource ConfigMap
   - Restarts Grafana to load datasource
   - Rollback script: `/tmp/rollback-grafana-cadvisor.sh`

### GitLab CI/CD Configuration

**File:** `.gitlab-ci/.gitlab-ci-monitoring.yml`

**Jobs Updated:**
- `deploy:loki` - Uses `23-deploy-loki-kubectl.yml`
- `deploy:promtail` - Uses `24-deploy-promtail-kubectl.yml`
- `deploy:exporters` - Uses `25-configure-grafana-loki.yml`

**Pipeline Stages:**
1. `terraform:plan:monitoring` - Plan infrastructure
2. `terraform:apply:monitoring` - Provision infrastructure
3. `deploy:loki` - Deploy Loki log aggregation
4. `deploy:promtail` - Deploy Promtail log collector
5. `deploy:exporters` - Deploy cAdvisor and configure Grafana
6. `verify:monitoring` - Verify all components
7. `notify:success/failure` - Notifications

## Critical Configuration Details

### Loki Configuration (Fixed for No Timeouts)

**Location:** ConfigMap `loki-config` in `monitoring` namespace

```yaml
common:
  compactor_address: http://loki:3100
  path_prefix: /loki
  replication_factor: 1
  storage:
    filesystem:
      chunks_directory: /loki/chunks
  ring:
    kvstore:
      store: inmemory  # Critical: prevents memberlist/Consul errors

limits_config:
  retention_period: 720h  # 30 days
  ingestion_rate_mb: 16
  ingestion_burst_size_mb: 32
  per_stream_rate_limit: 16MB

schema_config:
  configs:
    - from: "2024-01-01"
      store: boltdb-shipper
      object_store: filesystem
      schema: v12
      index:
        prefix: index_
        period: 24h

ruler:
  storage:
    type: local
    local:
      directory: /tmp/loki-rules  # Critical: prevents permission denied
```

**Deployment Configuration:**
```yaml
containers:
- name: loki
  image: grafana/loki:2.9.10
  securityContext:
    runAsUser: 0  # Critical: run as root for directory creation
    runAsGroup: 0
  args:
    - "-config.file=/etc/loki/loki-config.yaml"
    - "-target=all"  # CRITICAL: Monolithic mode prevents timeout errors
```

**Why Monolithic Mode?**
- Split targets (read, write, ingester, querier, query-frontend) require gRPC communication
- In single-node setup, gRPC communication fails with 5+ second timeouts
- Monolithic mode (`-target=all`) runs all components in one process
- Query times: 89ms-1.16s (down from 5+ seconds)

### Promtail Configuration (Fixed for Positions File)

**Location:** ConfigMap `promtail-config` in `monitoring` namespace

```yaml
data:
  config.yml: |  # Critical: key must be config.yml, not promtail.yaml
    server:
      http_listen_port: 3101

    positions:
      filename: /tmp/positions.yaml  # Critical: /var/log is read-only

    clients:
      - url: http://loki.monitoring.svc:3100/loki/api/v1/push

    scrape_configs:
      - job_name: kubernetes-pods
        kubernetes_sd_configs:
          - role: pod
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_name]
            target_label: pod
            action: replace
          - source_labels: [__meta_kubernetes_pod_namespace]
            target_label: namespace
            action: replace
          - source_labels: [__meta_kubernetes_pod_container_name]
            target_label: container
            action: replace
          # ... more labels
```

**DaemonSet Configuration:**
```yaml
containers:
- name: promtail
  image: grafana/promtail:2.9.10
  volumeMounts:
  - name: varlog
    mountPath: /var/log
    readOnly: true  # Critical: read-only mount
  - name: varlibdockercontainers
    mountPath: /var/lib/docker/containers
    readOnly: true
```

**Why /tmp for Positions File?**
- `/var/log` is mounted as read-only
- Promtail needs writable location for positions file
- `/tmp` is always writable

### cAdvisor Configuration (Fixed Mount Issues)

```yaml
containers:
- name: cadvisor
  image: gcr.io/cadvisor/cadvisor:v0.47.2
  args:
    - '--housekeeping_interval=10s'
    - '--enable_metrics=app,cpu,disk,diskIO,memory,network,process'
    - '--docker_only=true'
  volumeMounts:
  - name: varrun
    mountPath: /var/run
    # Critical: NO readOnly flag - allows service account mount
  - name: varlibdocker
    mountPath: /var/lib/docker
    readOnly: true
  - name: sysfs
    mountPath: /sys
    readOnly: true
  # Critical: NO rootfs mount - caused read-only errors
```

**Why These Mount Changes?**
1. **Removed rootfs mount** - Caused "read-only file system" errors
2. **Removed readOnly from /var/run** - Prevents service account mount errors
3. **Kept /var/lib/docker and /sys as read-only** - Security best practice

### Grafana Loki Datasource

**Location:** ConfigMap `grafana-loki-datasource` in `monitoring` namespace

```yaml
data:
  loki-datasource.yaml: |
    apiVersion: 1

    datasources:
    - name: Loki
      type: loki
      access: proxy
      url: http://loki.monitoring.svc:3100
      version: 1
      editable: false
      isDefault: false
      jsonData:
        maxLines: 1000
```

**Grafana Integration:**
- ConfigMap created with label `grafana_datasource: "1"`
- Grafana deployment patched to use ConfigMap datasources
- Grafana restarted to load datasource

## Deployment Instructions

### Manual Deployment

```bash
cd ansible/k8s

# Step 1: Deploy Loki
ansible-playbook -i inventory.ini 23-deploy-loki-kubectl.yml

# Step 2: Deploy Promtail
ansible-playbook -i inventory.ini 24-deploy-promtail-kubectl.yml

# Step 3: Deploy cAdvisor & Configure Grafana
ansible-playbook -i inventory.ini 25-configure-grafana-loki.yml
```

### GitLab CI/CD Deployment

1. **Configure CI/CD Variables:**
   - `YC_CLOUD_ID`
   - `YC_FOLDER_ID`
   - `YC_TOKEN`
   - `GITLAB_PAT` (Personal Access Token)
   - `GITLAB_USERNAME`
   - `SSH_PRIVATE_KEY`

2. **Run Pipeline Jobs in Order:**
   - Go to CI/CD → Pipelines
   - Click `terraform:plan:monitoring` → "Play"
   - Wait, then click `terraform:apply:monitoring` → "Play"
   - Click `deploy:loki` → "Play"
   - Click `deploy:promtail` → "Play"
   - Click `deploy:exporters` → "Play"
   - Click `verify:monitoring` → "Play"

## Using Grafana

### Access Grafana

```
URL: http://your-master-ip:3000
Username: admin
Password: prom-operator
```

### Query Logs in Grafana

1. **Open Grafana** → Click "Explore" (left sidebar)
2. **Select Loki datasource**
3. **Enter LogQL query:**

```logql
# All logs from ajasta namespace
{namespace="ajasta"}

# Error logs from backend
{namespace="ajasta", app="ajasta-backend"} |= "error"

# HTTP requests from frontend
{namespace="ajasta", app="ajasta-frontend"} |= "HTTP"

# Logs from specific pod
{namespace="ajasta", pod="ajasta-backend-xxx"}

# Filter by log level
{namespace="ajasta"} |= "ERROR" or |= "WARN"

# Regex matching
{namespace="ajasta"} |~ "status=5\d{2}"

# Time range queries
{namespace="ajasta"} |= "error" | line_format "{{.timestamp}} [{{.level}}] {{.message}}"
```

## Verification Commands

```bash
# Check Loki
kubectl get pods -n monitoring -l app=loki
kubectl logs -n monitoring -l app=loki -f

# Check Promtail
kubectl get pods -n monitoring -l app=promtail
kubectl logs -n monitoring -l app=promtail -f

# Check cAdvisor
kubectl get pods -n monitoring -l app=cadvisor
kubectl logs -n monitoring -l app=cadvisor -f

# Check Grafana
kubectl get pods -n monitoring -l app.kubernetes.io/name=grafana

# Test Loki API
kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -- \
  curl -s http://loki.monitoring.svc:3100/ready

# Query Loki for namespaces
kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -- \
  curl -s "http://loki.monitoring.svc:3100/loki/api/v1/label/namespace/values"

# View Loki ConfigMap
kubectl get cm loki-config -n monitoring -o yaml

# View Promtail ConfigMap
kubectl get cm promtail-config -n monitoring -o yaml

# View Grafana datasource ConfigMap
kubectl get cm grafana-loki-datasource -n monitoring -o yaml

# Port forward to access Loki locally
kubectl port-forward -n monitoring svc/loki 3100:3100

# Port forward to access Grafana locally
kubectl port-forward -n monitoring svc/kube-prometheus-grafana 3000:3000
```

## Rollback Procedures

Each Ansible playbook creates a rollback script on the master node:

```bash
# Rollback Loki
ssh ajasta@master-ip "sudo bash /tmp/rollback-loki-kubectl.sh"

# Rollback Promtail
ssh ajasta@master-ip "sudo bash /tmp/rollback-promtail-kubectl.sh"

# Rollback cAdvisor & Grafana config
ssh ajasta@master-ip "sudo bash /tmp/rollback-grafana-cadvisor.sh"
```

**Rollback Scripts Include:**
- Confirmation prompt
- Selective deletion (deployment, service, configmap, PVC)
- Option to preserve persistent data
- Re-deployment instructions

## Troubleshooting

### Loki Timeout Issues (FIXED)

**Problem:** Queries from Grafana timeout with "net/http: timeout awaiting response headers"

**Solution:** Use monolithic mode (`-target=all`) instead of split targets

```yaml
# BEFORE (causing timeouts):
args:
  - "-target=read,write,ingester,querier,query-frontend,compactor"

# AFTER (working):
args:
  - "-target=all"
```

### Promtail Positions File Errors (FIXED)

**Problem:** "error writing positions file... read-only file system"

**Solution:** Configure positions file to use `/tmp`

```yaml
positions:
  filename: /tmp/positions.yaml
```

### cAdvisor Mount Errors (FIXED)

**Problem:** "mkdirat ... read-only file system"

**Solution:** Remove rootfs mount and readOnly flag from /var/run

```yaml
volumeMounts:
- name: varrun
  mountPath: /var/run
  # NO readOnly flag
# NO rootfs mount
```

### Loki Permission Errors (FIXED)

**Problem:** "mkdir /loki/rules: permission denied"

**Solution:** Configure ruler storage in `/tmp` and run Loki as root

```yaml
ruler:
  storage:
    type: local
    local:
      directory: /tmp/loki-rules

securityContext:
  runAsUser: 0
  runAsGroup: 0
```

### Memberlist/Consul Errors (FIXED)

**Problem:** "dial tcp [::1]:8500: connect: connection refused"

**Solution:** Configure in-memory ring store

```yaml
common:
  ring:
    kvstore:
      store: inmemory
```

## Performance Tuning

### Loki Optimization

```yaml
# Increase ingestion rate
limits_config:
  ingestion_rate_mb: 32          # Increase from 16
  ingestion_burst_size_mb: 64    # Increase from 32

# Adjust retention
limits_config:
  retention_period: 2160h  # 90 days instead of 30
```

### Promtail Optimization

```yaml
# Increase resources
resources:
  limits:
    cpu: 500m  # Increase from 200m
    memory: 256Mi  # Increase from 128Mi

# Adjust batch size
clients:
  - url: http://loki.monitoring.svc:3100/loki/api/v1/push
    batch_wait: 1s
    batch_size: 1048576  # 1MB
```

## Key Differences from Helm-Based Implementation

| Feature | Helm-Based | kubectl-Based (This Implementation) |
|---------|------------|-------------------------------------|
| Helm Required | Yes | No |
| Grafana | New deployment | Uses existing kube-prometheus Grafana |
| Loki Mode | Split targets | Monolithic mode (no timeouts) |
| Config Storage | Helm values | ConfigMaps |
| Rollback | helm uninstall | Custom bash scripts |
| GitLab CI/CD | Helm commands | kubectl/ansible commands |

## Next Steps

1. **Create Custom Dashboards**
   - Import community dashboards (Node Exporter: 1860, cAdvisor: 15703)
   - Create application-specific dashboards
   - Add business metrics

2. **Set Up Alerts**
   - Configure Loki alerts in ruler
   - Set up Prometheus alerts
   - Integrate with notifications (Slack, email)

3. **Optimize Storage**
   - Monitor Loki disk usage
   - Adjust retention policies
   - Consider compaction settings

4. **Scale Monitoring Stack**
   - Add Loki replicas for HA (requires changing from in-memory ring)
   - Distribute Promtail load
   - Implement log sampling for high-volume environments

## Summary

This monitoring stack implementation provides:

✅ **Complete Log Aggregation** - Loki collects and stores logs from all pods
✅ **Automatic Log Collection** - Promtail runs on all nodes (DaemonSet)
✅ **Container Metrics** - cAdvisor provides container-level metrics
✅ **System Metrics** - node_exporter provides host-level metrics
✅ **Kubernetes Metrics** - kube-state-metrics provides object state
✅ **Visualization** - Grafana provides dashboards and log exploration
✅ **No Helm Dependency** - Uses kubectl manifests only
✅ **GitLab CI/CD Integration** - Automated deployment pipeline
✅ **Full Rollback Capability** - Scripts for complete rollback
✅ **Production Ready** - Optimized configurations, no timeout issues

**Log Flow:**
```
Pods → Promtail (DaemonSet) → Loki (Monolithic) → Grafana (Visualization)
```

**Metrics Flow:**
```
Containers → cAdvisor → Prometheus → Grafana
Hosts → node_exporter → Prometheus → Grafana
K8s Objects → kube-state-metrics → Prometheus → Grafana
```

**Files **
- `ansible/k8s/23-deploy-loki-kubectl.yml` 
- `ansible/k8s/24-deploy-promtail-kubectl.yml` 
- `ansible/k8s/25-configure-grafana-loki.yml` 
- `.gitlab-ci/.gitlab-ci-monitoring.yml` 
- `MONITORING_STACK_GUIDE.md` 
- `MONITORING_STACK_KUBECTL_IMPLEMENTATION.md` 

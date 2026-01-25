# Complete Grafana + Loki + Exporters Monitoring Stack

##  Overview

This guide covers the complete implementation of a production-ready monitoring and logging stack for the Ajasta application, including:

- **Loki** - Log aggregation system
- **Promtail** - Log collection agent
- **Grafana** - Visualization and dashboards
- **node_exporter** - System metrics (CPU, memory, disk, network)
- **cAdvisor** - Container metrics
- **kube-state-metrics** - Kubernetes object metrics
- **GitLab CI/CD** - Automated deployment

##  Architecture

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
│  │        (Log Aggregation)                │                │
│  └────────────┬────────────────────────────┘                │
│               │                                             │
│               ↓                                             │
│  ┌─────────────────────────────────────────┐                │
│  │            Grafana                      │                │
│  │     (Visualization & Dashboards)        │                │
│  └─────────────────────────────────────────┘                │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │node_exporter │  │  cAdvisor    │  │kube-state    │       │
│  │ (DaemonSet)  │  │ (DaemonSet)  │  │  (metrics)   │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │               │
│         └─────────────────┴─────────────────┘               │
│                           ↓                                 │
│                    Prometheus                               │
│                (Metrics Collection)                         │
│                           ↓                                 │
│                        Grafana                              │
└─────────────────────────────────────────────────────────────┘
                           ↑
                           │
                  GitLab CI/CD
          (Automated Deployment Pipeline)
```

##  Deployment Instructions

### Option 1: Manual Deployment (Ansible)

#### Step 1: Deploy Loki (Log Aggregation)

```bash
cd ansible/k8s
ansible-playbook -i inventory.ini 23-deploy-loki.yml
```

**What this does:**
- Deploys Loki via Helm chart
- Configures 10Gi persistent storage on Longhorn
- Sets 30-day log retention policy
- Exposes Loki API on port 3100

**Configuration:**
- Storage: 10Gi (expandable)
- Retention: 720h (30 days)
- Resources: 500m CPU / 512Mi RAM

**Verification:**
```bash
kubectl get pods -n monitoring -l app=loki
kubectl logs -n monitoring -l app=loki -f
```

#### Step 2: Deploy Promtail (Log Collection)

```bash
ansible-playbook -i inventory.ini 24-deploy-promtail.yml
```

**What this does:**
- Deploys Promtail as DaemonSet on all nodes
- Collects logs from all Kubernetes pods
- Adds Kubernetes metadata labels (namespace, pod, container)
- Sends logs to Loki

**Features:**
- Automatic service discovery
- Multi-line log handling
- Label enrichment

**Verification:**
```bash
kubectl get pods -n monitoring -l app=promtail
kubectl logs -n monitoring -l app=promtail -f
```

#### Step 3: Deploy Exporters & Configure Grafana

```bash
ansible-playbook -i inventory.ini 25-deploy-exporters.yml
```

**What this does:**
- Deploys node_exporter (system metrics)
- Deploys cAdvisor (container metrics)
- Deploys kube-state-metrics (K8s objects)
- Configures Grafana Loki datasource
- Restarts Grafana to load datasource

**Exporters Deployed:**
1. **node_exporter** - CPU, memory, disk, network metrics
2. **cAdvisor** - Container resource usage
3. **kube-state-metrics** - Kubernetes resource state

**Verification:**
```bash
kubectl get pods -n monitoring
kubectl get cm -n monitoring grafana-loki-datasource -o yaml
```

### Option 2: GitLab CI/CD Deployment

#### Prerequisites

Configure these GitLab CI/CD variables:

**Required Variables:**
```bash
# Yandex Cloud
YC_CLOUD_ID=your-cloud-id
YC_FOLDER_ID=your-folder-id
YC_TOKEN=your-oauth-token

# GitLab Authentication
GITLAB_PAT=your-personal-access-token
GITLAB_USERNAME=your-gitlab-username

# SSH Access
SSH_PRIVATE_KEY=your-ssh-private-key
```

#### Deployment via GitLab CI/CD

1. **Push to main branch**
   ```bash
   git add .
   git commit -m "Add monitoring stack with Loki + Exporters"
   git push origin main
   ```

2. **Go to GitLab CI/CD → Pipelines**
   - You'll see the monitoring jobs in the `monitoring-deploy` stage

3. **Run jobs in order:**
   - Click on `deploy:loki` → Click "Play" button
   - Wait for completion, then click `deploy:promtail` → "Play"
   - Finally, click `deploy:exporters` → "Play"

4. **Verify deployment**
   - Run `verify:monitoring` job
   - Check Grafana at http://your-master-ip:3000

#### CI/CD Pipeline Stages

```yaml
stages:
  - validate              # Terraform format, validate
  - plan                  # Terraform plan
  - monitoring-apply      # Provision infrastructure
  - monitoring-deploy     # Deploy monitoring stack
  - verify                # Verify all components
  - notify                # Success/failure notifications
```

##  Using Grafana

### Access Grafana

```
URL: http://your-master-ip:3000
Username: admin
Password: prom-operator
```

### View Logs in Grafana

1. **Login to Grafana**
   - Open http://your-master-ip:3000
   - Login with admin / prom-operator

2. **Go to Explore (left sidebar)**
   - Click "Explore" icon (magnifying glass)

3. **Select Loki datasource**

4. **Query logs:**
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

   # Time range queries
   {namespace="ajasta"} |= "error" | line_format "{{.timestamp}} [{{.level}}] {{.message}}"
   ```

5. **Useful LogQL Queries:**
   ```logql
   # Count errors per minute
   count_over_time({namespace="ajasta"} |= "error" [1m])

   # Get unique pod names with errors
   {namespace="ajasta"} |= "error" | label_format pod={{.pod}}

   # Filter by container
   {namespace="ajasta", container="backend"} |= "exception"

   # Regex matching
   {namespace="ajasta"} |~ "status=5\d{2}"

   # Combine labels
   {namespace=~"ajasta|monitoring", container!~"promtail|loki"}
   ```

### Create Dashboards

1. **Import Dashboard**
   - Click "+" → "Import"
   - Enter dashboard ID (e.g., 13639 for Node Exporter Full)
   - Click "Load"

2. **Create Custom Dashboard**
   - Click "+" → "Dashboard"
   - Add panels for:
     - CPU usage
     - Memory usage
     - Disk I/O
     - Network traffic
     - Container metrics

3. **Add Loki Panel**
   - Add new panel
   - Select Loki datasource
   - Enter LogQL query
   - Choose visualization (Table, Logs, Time series)

### Dashboards to Import

**Recommended Dashboards:**
- **Node Exporter Full**: ID `1860` or `13639`
- **Kubernetes Cluster Monitoring**: ID `7249`
- **Loki Built-in**: Available in Grafana
- **cAdvisor**: ID `15703`

## 🔍 Monitoring Stack Components

### Loki Configuration

**Location:** `/tmp/loki-values.yaml` (generated during deployment)

**Key Settings:**
```yaml
loki:
  storage:
    type: filesystem
  limits_config:
    retention_period: 720h  # 30 days
    ingestion_rate_mb: 16
    per_stream_rate_limit: 16MB
  schema_config:
    - from: "2024-01-01"
      store: boltdb-shipper
      schema: v12
```

**Access Loki API:**
```bash
# Port forward
kubectl port-forward -n monitoring svc/loki 3100:3100

# Test API
curl http://localhost:3100/ready
curl http://localhost:3100/labels

# Query logs
curl -G http://localhost:3100/loki/api/v1/query \
  --data-urlencode 'query={namespace="ajasta"}'
```

### Promtail Configuration

**Location:** ConfigMap `promtail-config` in `monitoring` namespace

**Key Features:**
- **Pod Discovery:** Automatically discovers all pods
- **Label Enrichment:** Adds Kubernetes metadata
- **Multi-line Handling:** Handles stack traces correctly
- **Journal Support:** Can collect systemd journals

**View/Edit Config:**
```bash
kubectl get cm promtail-config -n monitoring -o yaml
kubectl edit cm promtail-config -n monitoring
```

### Exporters

**node_exporter:**
```bash
# Port: 9100
# Endpoints:
kubectl get pods -n monitoring -l app=node-exporter
kubectl port-forward <pod> 9100:9100
curl http://localhost:9100/metrics | grep node_cpu
```

**cAdvisor:**
```bash
# Port: 8080
# Endpoints:
kubectl get pods -n monitoring -l app=cadvisor
kubectl port-forward <pod> 8080:8080
curl http://localhost:8080/metrics | grep container_cpu
```

**kube-state-metrics:**
```bash
# Port: 8080
# Endpoints:
kubectl get pods -n monitoring -l app.kubernetes.io/name=kube-state-metrics
curl http://kube-state-metrics.monitoring.svc:8080/metrics | grep kube_pod
```

##  Troubleshooting

### Common Issues

#### 1. Loki is not receiving logs

**Check Promtail:**
```bash
kubectl logs -n monitoring -l app=promtail -f
# Look for: "error adding stream"
```

**Check Loki:**
```bash
kubectl logs -n monitoring -l app=loki -f
# Look for: "error pushing entries"
```

**Test connection:**
```bash
kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -- \
  curl -s http://loki.monitoring.svc:3100/ready
```

#### 2. Grafana Loki datasource not working

**Check datasource:**
```bash
kubectl get cm -n monitoring grafana-loki-datasource -o yaml
```

**Restart Grafana:**
```bash
kubectl rollout restart deployment kube-prometheus-grafana -n monitoring
kubectl logs -n monitoring -l app.kubernetes.io/name=grafana -f
```

#### 3. Exporters not scraping

**Check pods:**
```bash
kubectl get pods -n monitoring
```

**Check ServiceMonitor:**
```bash
kubectl get servicemonitor -n monitoring
```

**Test endpoints:**
```bash
kubectl port-forward -n monitoring svc/node-exporter 9100:9100 &
curl http://localhost:9100/metrics
```

### Debug Commands

```bash
# View all monitoring components
kubectl get all -n monitoring

# Check Loki storage
kubectl exec -n monitoring <loki-pod> -- df -h /loki

# View Promtail targets
kubectl exec -n monitoring <promtail-pod> -- wget -qO- http://localhost:3101/targets

# Check logs flow
kubectl logs -n monitoring <promtail-pod> | grep "level=error"

# Test Loki query
kubectl port-forward -n monitoring svc/loki 3100:3100 &
curl -G http://localhost:3100/loki/api/v1/label/job/values

# View Grafana logs
kubectl logs -n monitoring -l app.kubernetes.io/name=grafana -f

# Check disk usage
kubectl exec -n monitoring <loki-pod> -- du -sh /loki/
```

## 📈 Performance Tuning

### Loki Optimization

**Increase ingestion rate:**
```yaml
limits_config:
  ingestion_rate_mb: 32          # Increase from 16
  ingestion_burst_size_mb: 64    # Increase from 32
```

**Adjust retention:**
```yaml
limits_config:
  retention_period: 2160h  # 90 days instead of 30
```

**Add replicas (for HA):**
```yaml
loki:
  commonConfig:
    replication_factor: 3
```

### Promtail Optimization

**Increase scrape rate:**
```yaml
promtail:
  resources:
    limits:
      cpu: 500m  # Increase from 200m
```

**Adjust batch size:**
```yaml
promtail:
  config:
      client:
        batch_wait: 1s
        batch_size: 1048576  # 1MB
```

## 🔄 Rollback Procedures

### Manual Rollback

Each playbook creates a rollback script:

```bash
# Rollback Loki
ssh ajasta@master-ip "sudo bash /tmp/rollback-loki.sh"

# Rollback Promtail
ssh ajasta@master-ip "sudo bash /tmp/rollback-promtail.sh"

# Rollback Exporters
ssh ajasta@master-ip "sudo bash /tmp/rollback-exporters.sh"
```

### GitLab CI/CD Rollback

Use the built-in rollback jobs in GitLab CI/CD:

1. Go to CI/CD → Pipelines
2. Find your pipeline
3. Click on rollback stage:
   - `monitoring:rollback:loki:staging`
   - `monitoring:rollback:promtail:staging`
   - `monitoring:rollback:exporters:staging`
4. Click "Play" button

## 📚 Useful Resources

### Official Documentation
- [Loki Documentation](https://grafana.com/docs/loki/latest/)
- [Promtail Documentation](https://grafana.com/docs/loki/latest/clients/promtail/)
- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [LogQL Guide](https://grafana.com/docs/loki/latest/logql/)

### GitLab CI/CD
- [GitLab CI/CD Documentation](https://docs.gitlab.com/ee/ci/)
- [GitLab Terraform Integration](https://docs.gitlab.com/ee/integration/terraform.html)

### Exporters
- [node_exporter](https://github.com/prometheus/node_exporter)
- [cAdvisor](https://github.com/google/cadvisor)
- [kube-state-metrics](https://github.com/kubernetes/kube-state-metrics)


##  Next Steps

1. **Create Custom Dashboards**
   - Build application-specific dashboards
   - Add business metrics
   - Create alerting rules

2. **Set Up Alerts**
   - Configure Loki alerts
   - Set up Prometheus alerts
   - Integrate with notifications (Slack, email)

3. **Optimize Storage**
   - Monitor Loki disk usage
   - Adjust retention policies
   - Consider compaction settings

4. **Scale Monitoring Stack**
   - Add Loki replicas for HA
   - Distribute Promtail load
   - Implement log sampling for high-volume environments

5. **Integrate with Existing Tools**
   - Sentry for error tracking
   - PagerDuty for on-call
   - Custom webhooks for events

---

**Quick Reference:**

| Component | Port | Namespace | Command to Check |
|-----------|------|-----------|------------------|
| Loki | 3100 | monitoring | `kubectl get pods -n monitoring -l app=loki` |
| Promtail | 3101 | monitoring | `kubectl get ds -n monitoring promtail` |
| Grafana | 3000 | monitoring | `kubectl get pods -n monitoring -l app.kubernetes.io/name=grafana` |
| node_exporter | 9100 | monitoring | `kubectl get ds -n monitoring node-exporter` |
| cAdvisor | 8080 | monitoring | `kubectl get ds -n monitoring cadvisor` |

**Support:**
For issues or questions, check the logs in `/tmp/` on the master node or run the verification playbook.


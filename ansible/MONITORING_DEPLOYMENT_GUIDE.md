# Monitoring Stack Deployment Guide

Complete guide for deploying Prometheus + Grafana monitoring with exporters integrated into GitLab CI/CD.

## Overview

This monitoring stack provides:
- **Prometheus** - Metrics collection and storage
- **Grafana** - Visualization and dashboards
- **Alertmanager** - Alert routing and notifications
- **Exporters** - Node, Kube-State-Metrics, cAdvisor
- **Integration** - GitLab CI/CD automated deployment

## Architecture

```
GitLab CI/CD
    ↓
Ansible Roles
    ↓
Kubernetes Cluster
    ├── Prometheus Operator
    ├── Grafana
    ├── Alertmanager
    ├── Exporters
    └── Application Metrics
```

## Prerequisites

### Required
- Kubernetes cluster >= 1.29
- kubectl configured
- Helm 3.x installed
- Longhorn StorageClass configured
- GitLab CI/CD configured

### GitLab CI/CD Variables

Set these in your GitLab project: Settings > CI/CD > Variables

**Required Variables**:
```bash
# Yandex Cloud (existing)
YC_CLOUD_ID=your-cloud-id
YC_FOLDER_ID=your-folder-id
YC_TOKEN=your-token

# GitLab Authentication (existing)
GITLAB_PAT=your-pat
GITLAB_USERNAME=your-username

# Monitoring Specific
GRAFANA_ADMIN_PASSWORD=your-secure-password
SLACK_WEBHOOK_URL=https://hooks.slack.com/YOUR_WEBHOOK
ALERT_EMAIL=devops@ajasta.top

# Storage Configuration
PROMETHEUS_STORAGE_SIZE=50Gi
GRAFANA_STORAGE_SIZE=10Gi
PROMETHEUS_RETENTION=15d
```

## Quick Start

### Option 1: Deploy via GitLab CI/CD

1. **Set CI/CD Variables** in GitLab

2. **Push to main branch**:
```bash
git add .
git commit -m "Enable monitoring deployment"
git push origin main
```

3. **Monitor pipeline** in GitLab: CI/CD > Pipelines

4. **Access dashboards**:
- Prometheus: https://prometheus.ajasta.top
- Grafana: https://grafana.ajasta.top (admin / YOUR_PASSWORD)
- Alertmanager: https://alertmanager.ajasta.top

### Option 2: Deploy Manually

```bash
# Set environment variables
export GRAFANA_ADMIN_PASSWORD="your-password"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/YOUR_WEBHOOK"
export ALERT_EMAIL="devops@ajasta.top"

# Deploy monitoring stack
cd ansible
ansible-playbook deploy-monitoring-stack.yml -v
```

## Components

### 1. Prometheus Operator

**Deployed via**: `prometheus_operator` role

**Features**:
- Automated Prometheus management
- ServiceMonitor CRD for service discovery
- PrometheusRule CRD for alerting
- Persistent storage (50Gi, 15-day retention)

**Access**:
- External: https://prometheus.ajasta.top
- Internal: http://prometheus-operated.monitoring.svc.cluster.local:9090

**Key Metrics**:
- Cluster health
- Node performance
- Pod resources
- Application metrics

### 2. Grafana

**Deployed via**: `grafana` role

**Features**:
- Pre-configured dashboards
- Multiple data sources
- User authentication
- Plugin support

**Pre-installed Dashboards**:
1. **Kubernetes Cluster Overview** - Overall cluster health
2. **Node Exporter Full** - Node CPU, memory, disk, network
3. **Kubelet** - Kubelet metrics
4. **Nginx Ingress** - Ingress performance
5. **Kubernetes Pod Metrics** - Pod resource usage
6. **Ajasta Backend Metrics** - Spring Boot application metrics
7. **JVM Micrometer** - JVM memory, threads, GC
8. **PostgreSQL Database** - Database performance

**Access**:
- URL: https://grafana.ajasta.top
- Credentials: admin / YOUR_PASSWORD

### 3. Alertmanager

**Deployed via**: `prometheus_operator` role

**Features**:
- Email alerts
- Slack notifications
- Alert grouping
- Silencing and inhibition

**Alert Channels**:
- Slack: #alerts channel
- Email: devops@ajasta.top

**Access**:
- External: https://alertmanager.ajasta.top
- Internal: http://alertmanager-operated.monitoring.svc.cluster.local:9093

### 4. Exporters

#### Node Exporter
- **Metrics**: CPU, memory, disk, network
- **Port**: 9100
- **DaemonSet**: Runs on all nodes

#### Kube-State-Metrics
- **Metrics**: Kubernetes objects (pods, services, deployments)
- **Port**: 8080
- **Deployment**: Single replica

#### cAdvisor
- **Metrics**: Container metrics
- **Port**: 10250
- **Built-in**: Runs with kubelet

#### Application Metrics
- **Spring Boot Actuator**: /actuator/prometheus
- **Custom metrics**: Bookings, payments, sessions
- **Port**: 8090

## Alerting Rules

### Critical Alerts (Immediate)

**KubernetesClusterNotReady**
- Cluster not ready for 10 minutes

**KubernetesNodeDown**
- Node down for 5 minutes

**KubernetesPodCrashLooping**
- Pod crash looping

**AjastaBackendHighErrorRate**
- Error rate > 5% for 5 minutes

### Warning Alerts (5 minutes)

**KubernetesNodeCPUUsageHigh**
- CPU > 80%

**KubernetesNodeMemoryUsageHigh**
- Memory > 85%

**KubernetesDiskSpaceLow**
- Disk < 15%

**AjastaBackendHighLatency**
- P95 latency > 1s

**AjastaDatabaseConnectionsHigh**
- DB connections > 80%

## Dashboards

### Accessing Dashboards

1. Login to Grafana: https://grafana.ajasta.top
2. Navigate to: Dashboards > Browse
3. Select a dashboard folder:
   - Kubernetes
   - Nodes
   - Applications
   - Databases

### Key Dashboards

**Kubernetes Cluster Overview**
- Cluster health score
- Resource usage overview
- Node status
- Pod health

**Node Exporter Full**
- CPU usage, load average
- Memory usage, swap
- Disk I/O, filesystem
- Network I/O, connections

**Ajasta Backend Metrics**
- Request rate
- P95 latency
- Error rate
- Database connection pool

**JVM Micrometer**
- Heap memory usage
- Thread count
- GC statistics
- Class loading

## Configuration

### Update Retention

Edit `deploy-monitoring-stack.yml`:
```yaml
prometheus_retention: "30d"  # Keep 30 days instead of 15
```

### Update Storage Size

Edit `deploy-monitoring-stack.yml`:
```yaml
prometheus_storage_size: "100Gi"  # Increase to 100Gi
```

### Add Custom Alerts

Edit `roles/prometheus_operator/tasks/main.yml`:
```yaml
- name: Create Prometheus alerting rules
  kubernetes.core.k8s:
    definition:
      spec:
        groups:
          - name: custom-alerts
            rules:
              - alert: MyCustomAlert
                expr: my_metric > 100
                for: 10m
                labels:
                  severity: warning
                annotations:
                  summary: "My custom alert"
```

### Configure Email Alerts

Edit `deploy-monitoring-stack.yml`:
```yaml
alertmanager_config:
  route:
    receivers:
      - name: 'critical'
        email_configs:
          - to: 'your-email@example.com'
            from: 'alertmanager@example.com'
            smarthost: 'smtp.example.com:587'
            auth_username: 'your-username'
            auth_password: 'your-password'
```

## Troubleshooting

### Check Prometheus Status

```bash
# Check pods
kubectl get pods -n monitoring -l app=prometheus

# Check logs
kubectl logs -n monitoring prometheus-prometheus-0

# Port forward
kubectl port-forward -n monitoring svc/prometheus-operated 9090:9090
```

### Check Grafana Status

```bash
# Check pods
kubectl get pods -n monitoring -l app.kubernetes.io/name=grafana

# Check logs
kubectl logs -n monitoring deployment/grafana

# Port forward
kubectl port-forward -n monitoring svc/grafana 3000:3000
```

### Verify Targets

1. Open Prometheus: https://prometheus.ajasta.top
2. Go to: Status > Targets
3. Verify all targets are "UP"

### Verify Alerts

1. Open Alertmanager: https://alertmanager.ajasta.top
2. Check for active alerts
3. Verify notification channels

### Common Issues

**Issue**: Prometheus out of memory
```yaml
# Increase limits
prometheus_resources:
  limits:
    memory: "8Gi"
```

**Issue**: Disk full
```bash
# Reduce retention
prometheus_retention: "7d"
```

**Issue**: Grafana not connecting to Prometheus
```bash
# Check datasources
kubectl get configmap -n monitoring grafana-datasources -o yaml
```

## Performance Tuning

### For Large Clusters (100+ nodes)

```yaml
prometheus_resources:
  requests:
    memory: "4Gi"
    cpu: "1000m"
  limits:
    memory: "8Gi"
    cpu: "2000m"

prometheus_scrape_interval: "15s"
prometheus_evaluation_interval: "15s"
```

### For High Availability

```yaml
prometheus_replicas: 3
alertmanager_replicas: 3
```

Add Thanos for long-term storage.

## GitLab CI/CD Pipeline

### Pipeline Stages

1. **terraform:plan:monitoring** - Plan infrastructure
2. **terraform:apply:monitoring** - Provision storage
3. **deploy:prometheus** - Deploy Prometheus Operator
4. **deploy:grafana** - Deploy Grafana
5. **deploy:exporters** - Deploy Node Exporter
6. **verify:monitoring** - Verify deployment
7. **notify:success/failure** - Send notifications

### Manual Trigger

Go to: CI/CD > Pipelines > Run Pipeline

### Monitoring Pipeline Status

Monitor pipeline in GitLab: https://gitlab.com/your-org/your-project/-/pipelines

## Maintenance

### Update Prometheus Version

Edit `roles/prometheus_operator/defaults/main.yml`:
```yaml
prometheus_operator_version: "0.69.0"
```

Re-run pipeline or playbook.

### Backup Grafana Dashboards

```bash
# Export all dashboards
kubectl exec -n monitoring deployment/grafana -- \
  grafana-cli admin export-dashboard > /tmp/grafana-dashboards.json
```

### Backup Prometheus Data

```bash
# Snapshot Prometheus data
kubectl exec -n monitoring prometheus-prometheus-0 -- \
  prometheus tsdb snapshot /prometheus/snapshots/
```

## Scaling

### Horizontal Scaling

Not recommended for Prometheus. Use:
- Thanos for long-term storage
- VictoriaMetrics for high scalability
- Mimir for massive scale

### Vertical Scaling

```yaml
prometheus_resources:
  limits:
    memory: "16Gi"
    cpu: "4000m"
```

## Next Steps

1. **Configure alerts** for your specific needs
2. **Create custom dashboards** for your applications
3. **Set up alert routing** to Slack/Email
4. **Test alert delivery**
5. **Review metrics** regularly
6. **Tune retention** based on storage needs
7. **Set up backup** for critical dashboards

## Support

- **Issues**: https://github.com/RurikV/ajasta-ansible-automation/issues
- **Email**: devops@ajasta.top
- **Slack**: #devops-alerts

## References

- Prometheus: https://prometheus.io/docs/
- Grafana: https://grafana.com/docs/
- Alertmanager: https://prometheus.io/docs/alerting/latest/alertmanager/
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

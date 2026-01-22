# prometheus_operator Ansible Role

[![Galaxy](https://img.shields.io/badge/galaxy-ajasta.prometheus_operator-blue.svg)](https://galaxy.ansible.com/ajasta/prometheus_operator)

Deploy Prometheus Operator for comprehensive Kubernetes monitoring.

## Features

- ✅ Prometheus Operator deployment via Helm
- ✅ Alertmanager with email and Slack notifications
- ✅ ServiceMonitors for auto-discovery
- ✅ Pre-configured alerting rules
- ✅ Persistent storage with Longhorn
- ✅ TLS-enabled Ingress
- ✅ Multi-namespace monitoring support
- ✅ Application metrics scraping

## Requirements

- Kubernetes cluster >= 1.29
- Helm 3.x
- kubectl configured
- StorageClass configured (Longhorn recommended)

## Role Variables

### Prometheus Configuration

```yaml
# Kubernetes namespace
kubernetes_namespace: monitoring

# Prometheus Operator
prometheus_operator_version: "0.68.0"
prometheus_replicas: 1

# Storage
prometheus_storage_size: "50Gi"
prometheus_retention: "15d"

# Resources
prometheus_resources:
  requests:
    memory: "2Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "1000m"
```

### Alertmanager Configuration

```yaml
alertmanager_enabled: true
alertmanager_replicas: 1
alertmanager_storage_size: "2Gi"

# Alert configuration
alertmanager_config:
  global:
    resolve_timeout: 5m
  route:
    group_by: ['alertname', 'cluster']
    receivers:
      - name: 'critical'
        webhook_configs:
          - url: "https://hooks.slack.com/YOUR_WEBHOOK"
        email_configs:
          - to: "devops@ajasta.top"
```

### Monitoring Targets

```yaml
# Node Exporter
node_exporter_enabled: true
node_exporter_port: 9100

# Kube-State-Metrics
kube_state_metrics_enabled: true

# Application monitoring
app_monitoring_enabled: true
app_namespace: ajasta
app_scrape_path: /actuator/prometheus
app_scrape_port: 8090
```

### Ingress Configuration

```yaml
prometheus_ingress_enabled: true
prometheus_ingress_host: "prometheus.ajasta.top"

alertmanager_ingress_enabled: true
alertmanager_ingress_host: "alertmanager.ajasta.top"
```

## Example Playbook

### Basic Deployment

```yaml
---
- hosts: localhost
  gather_facts: false
  roles:
    - role: prometheus_operator
      vars:
        kubernetes_namespace: monitoring
        prometheus_storage_size: "50Gi"
        prometheus_retention: "15d"
```

### Production Deployment with Alerts

```yaml
---
- hosts: localhost
  gather_facts: false
  roles:
    - role: prometheus_operator
      vars:
        kubernetes_namespace: monitoring
        prometheus_replicas: 1
        prometheus_storage_size: "100Gi"
        alertmanager_enabled: true
        alertmanager_config:
          route:
            receivers:
              - name: 'critical'
                slack_configs:
                  - api_url: "{{ slack_webhook_url }}"
                    channel: '#alerts'
              - name: 'warning'
                email_configs:
                  - to: 'ops@ajasta.top'
        app_monitoring_enabled: true
        app_namespace: ajasta
```

## Included Alert Rules

### Critical Alerts (Immediate)
- **KubernetesClusterNotReady** - Cluster not ready for 10min
- **KubernetesNodeDown** - Node down for 5min
- **KubernetesPodCrashLooping** - Pod crash looping
- **AjastaBackendHighErrorRate** - Error rate > 5%

### Warning Alerts (5 minutes)
- **KubernetesNodeCPUUsageHigh** - CPU > 80%
- **KubernetesNodeMemoryUsageHigh** - Memory > 85%
- **KubernetesDiskSpaceLow** - Disk < 15%
- **AjastaBackendHighLatency** - P95 latency > 1s
- **AjastaDatabaseConnectionsHigh** - DB connections > 80%

## Monitoring Targets

After deployment, Prometheus will automatically scrape:

1. **Infrastructure**:
   - All nodes: Node Exporter (:9100)
   - All pods: cAdvisor (:10250)
   - K8s resources: Kube-State-Metrics (:8080)

2. **Application**:
   - Ajasta Backend: Spring Boot Actuator (/actuator/prometheus)

3. **Kubernetes Components**:
   - API Server
   - Kubelet
   - Controller Manager (if enabled)
   - Scheduler (if enabled)

## Access

### Prometheus UI

```bash
# Port forward
kubectl port-forward -n monitoring svc/prometheus-operated 9090:9090

# Access via Ingress
https://prometheus.ajasta.top
```

### Alertmanager UI

```bash
# Port forward
kubectl port-forward -n monitoring svc/alertmanager-operated 9093:9093

# Access via Ingress
https://alertmanager.ajasta.top
```

## ServiceMonitors

This role creates ServiceMonitors for:

- Node Exporter (scrapes :9100/metrics)
- Kube-State-Metrics (scrapes :8080/metrics)
- cAdvisor (scrapes :10250/metrics)
- Ajasta Backend (scrapes :8090/actuator/prometheus)

## Custom ServiceMonitors

To add custom monitoring, label your services:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-app
  namespace: my-namespace
  labels:
    prometheus: enabled  # Enable monitoring
spec:
  selector:
    app: my-app
  ports:
    - port: 8080
      name: metrics
```

## Configuration Management

### Update Retention

```yaml
prometheus_retention: "30d"  # Keep 30 days
prometheus_retention_size: "100GB"  # Max 100GB
```

### Update Scrape Interval

```yaml
prometheus_scrape_interval: "15s"  # Scrape every 15s
prometheus_evaluation_interval: "15s"  # Evaluate rules every 15s
```

### Add Custom Alert Rules

Edit `tasks/main.yml` and add rules to `PrometheusRule` resource.

## Troubleshooting

### Check Prometheus Status

```bash
kubectl get pods -n monitoring -l app=prometheus
kubectl logs -n monitoring prometheus-prometheus-0

# Check configuration
kubectl exec -n monitoring prometheus-prometheus-0 -- promtool check config /etc/prometheus/config_out/prometheus.env.yaml
```

### Check Alertmanager Status

```bash
kubectl get pods -n monitoring -l app=alertmanager
kubectl logs -n monitoring alertmanager-alertmanager-0
```

### Verify Targets

```bash
# Port forward to Prometheus
kubectl port-forward -n monitoring svc/prometheus-operated 9090:9090

# Open http://localhost:9090/targets
```

### Common Issues

**Issue**: Out of memory
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

**Issue**: Alerts not firing
```bash
# Check Alertmanager configuration
kubectl get secret -n monitoring alertmanager-prometheus-alertmanager -o jsonpath='{.data.alertmanager\.yaml}' | base64 -d

# Check Prometheus rules
kubectl prometheus -n monitoring
```

## Metrics Integration

### Spring Boot Application

Add to your Spring Boot application:

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### Custom Metrics

```java
// Java
@Component
public class BookingMetrics {
    private final Counter bookingCounter;

    public BookingMetrics(MeterRegistry registry) {
        this.bookingCounter = Counter.builder("ajasta_bookings_total")
            .description("Total bookings created")
            .register(registry);
    }

    public void recordBooking() {
        bookingCounter.increment();
    }
}
```

## Scaling

### High Availability

For production HA, deploy multiple replicas:

```yaml
prometheus_replicas: 3
alertmanager_replicas: 3
```

Add Thanos for long-term storage and global query view.

### Performance Tuning

```yaml
# Increase resources for large clusters
prometheus_resources:
  requests:
    memory: "4Gi"
    cpu: "1000m"
  limits:
    memory: "8Gi"
    cpu: "2000m"

# Tune compaction
prometheus_retention: "15d"
```

## Uninstall

```bash
# Remove Helm release
helm uninstall prometheus-operator -n monitoring

# Remove PVCs
kubectl delete pvc -n monitoring -l app=prometheus
kubectl delete pvc -n monitoring -l app=alertmanager

# Remove namespace
kubectl delete namespace monitoring
```

## License

MIT

## Author Information

- **Author**: Ajasta DevOps Team
- **Email**: devops@ajasta.top

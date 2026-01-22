# Monitoring Stack Architecture - Prometheus + Grafana

## Overview

Complete monitoring solution for Ajasta application and Kubernetes infrastructure using Prometheus, Grafana, and integrated exporters deployed via Ansible and Terraform with GitLab CI/CD automation.

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                       GitLab CI/CD                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Monitoring Pipeline Stage                               │  │
│  │  - Terraform plan/apply (storage)                        │  │
│  │  - Ansible deploy monitoring stack                       │  │
│  │  - Deploy Grafana dashboards                             │  │
│  │  - Configure alerting rules                              │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                          │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Monitoring Namespace                        │  │
│  │                                                          │  │
│  │  ┌──────────────────┐  ┌──────────────────────────┐      │  │
│  │  │ Prometheus       │  │ Grafana                  │      │  │
│  │  │ Operator         │  │ - Dashboards             │      │  │
│  │  │                  │  │ - Users (admin/guest)    │      │  │
│  │  │ ┌──────────────┐ │  │ - Data Sources           │      │  │
│  │  │ │ Prometheus   │ │  └──────────────────────────┘      │  │
│  │  │ │ - Scrape     │ │                                    │  │
│  │  │ │ - Store      │ │  ┌──────────────────────────┐      │  │
│  │  │ │ - Alert      │ │  │ Alertmanager             │      │  │
│  │  │ └──────────────┘ │  │ - Email alerts           │      │  │
│  │  │                  │  │ - Slack notifications    │      │  │
│  │  │ ┌──────────────┐ │  └──────────────────────────┘      │  │
│  │  │ │ Alertmanager │ │                                    │  │
│  │  │ └──────────────┘ │                                    │  │
│  │  └──────────────────┘                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Exporters (per node)                        │  │
│  │                                                          │  │
│  │  ┌─────────────┐ ┌──────────────┐ ┌─────────────────┐    │  │
│  │  │ Node        │ │ Kube-State   │ │ cAdvisor        │    │  │
│  │  │ Exporter    │ │ Metrics      │ │ (container)     │    │  │
│  │  │             │ │              │ │                 │    │  │
│  │  │ /metrics    │ │ /metrics     │ │ /metrics        │    │  │
│  │  │ :9100       │ │ :8080        │ │ :10250          │    │  │
│  │  └─────────────┘ └──────────────┘ └─────────────────┘    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Application Metrics (Ajasta Backend)             │  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────────┐      │  │
│  │  │ Spring Boot Actuator                           │      │  │
│  │  │ - /actuator/prometheus                         │      │  │
│  │  │ - /actuator/health                             │      │  │
│  │  │ - /actuator/metrics                            │      │  │
│  │  │                                                │      │  │
│  │  │ Metrics:                                       │      │  │
│  │  │ - JVM: heap, threads, GC                       │      │  │
│  │  │ - HTTP: requests, latency, errors              │      │  │
│  │  │ - DB: connections, query time                  │      │  │
│  │  │ - Custom: bookings, payments                   │      │  │
│  │  └────────────────────────────────────────────────┘      │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Persistent Storage                          │
│  ┌──────────────────────┐  ┌────────────────────────────┐       │
│  │ Prometheus TSDB      │  │ Grafana Database           │       │
│  │ (Longhorn PVC)       │  │ (Longhorn PVC)             │       │
│  │ - 50Gi               │  │ - 10Gi                     │       │
│  │ - 15 day retention   │  │ - SQLite                   │       │
│  └──────────────────────┘  └────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### 1. Prometheus Operator
**Purpose**: Manages Prometheus instances and monitoring configuration

**Features**:
- Automated Prometheus deployment
- ServiceMonitor CRD for service discovery
- PrometheusRule CRD for alerting rules
- Thanos support for long-term storage (optional)

**Resources**:
- StatefulSet for Prometheus
- ServiceMonitor for auto-discovery
- PrometheusRule for alerting
- Secret for configuration

### 2. Grafana
**Purpose**: Visualization and dashboards

**Features**:
- Pre-built dashboards for:
  - Kubernetes cluster health
  - Node performance
  - Pod resource usage
  - Ajasta application metrics
  - JVM metrics
  - Database performance
- Multiple users (admin, viewer, guest)
- Persistent storage
- Plugin support

**Dashboards**:
1. **Kubernetes Cluster Overview**
   - Cluster health
   - Resource usage
   - Node status
   - Pod health

2. **Node Performance**
   - CPU, memory, disk
   - Network I/O
   - Load average

3. **Ajasta Application**
   - Request rate, latency
   - Error rate
   - JVM metrics
   - Database connections

4. **PostgreSQL Database**
   - Connection pool
   - Query performance
   - Replication lag

5. **Kubernetes Resources**
   - Pods, deployments, services
   - Resource quotas
   - Events

### 3. Exporters

#### Node Exporter
**Purpose**: System-level metrics

**Metrics**:
- CPU usage, load average
- Memory usage, swap
- Disk I/O, filesystem
- Network I/O, connections
- System info

**Endpoint**: `:9100/metrics`

#### Kube-State-Metrics
**Purpose**: Kubernetes object metrics

**Metrics**:
- Pod status, phase
- Deployment status
- Service endpoints
- Resource quotas
- Custom resource status

**Endpoint**: `:8080/metrics`

#### cAdvisor
**Purpose**: Container metrics

**Metrics**:
- Container CPU, memory
- Container network I/O
- Filesystem usage
- Container health

**Endpoint**: `:10250/metrics`

### 4. Alertmanager
**Purpose**: Alert routing and notification

**Alert Channels**:
- Email (SMTP)
- Slack webhook
- PagerDuty (optional)
- Custom webhooks

**Alert Rules**:
- Critical alerts (immediate notification):
  - Cluster node down
  - Pod crash looping
  - Database connection lost
  - API error rate > 50%

- Warning alerts (within 5 minutes):
  - High CPU usage > 80%
  - High memory usage > 85%
  - Disk space < 15%
  - API latency > 1s

- Info alerts (hourly digest):
  - Certificate expiration < 30 days
  - Pod restarts
  - Deployment changes

### 5. Application Metrics
**Spring Boot Actuator** (Ajasta Backend)

**Endpoints**:
- `/actuator/prometheus` - Prometheus format metrics
- `/actuator/health` - Health checks
- `/actuator/metrics` - All metrics

**Custom Metrics**:
- `ajasta_bookings_total` - Total bookings created
- `ajasta_payments_total` - Total payments processed
- `ajasta_payment_errors_total` - Payment errors
- `ajasta_active_sessions` - Active user sessions

## GitLab CI/CD Integration

### Pipeline Stages

```yaml
stages:
  - terraform-plan
  - terraform-apply
  - deploy-monitoring
  - verify
  - cleanup
```

### Jobs

1. **terraform:plan:monitoring**
   - Plans monitoring infrastructure (PVCs, storage)
   - Uses Terraform to manage Longhorn storage

2. **terraform:apply:monitoring**
   - Applies storage configuration
   - Creates PVCs for Prometheus and Grafana

3. **deploy:monitoring**
   - Deploys Prometheus Operator
   - Deploys Grafana
   - Configures ServiceMonitors
   - Deploys exporters
   - Creates dashboards
   - Configures alerting

4. **verify:monitoring**
   - Verifies Prometheus is scraping targets
   - Verifies Grafana dashboards are accessible
   - Tests alert delivery

5. **cleanup:monitoring** (manual)
   - Removes monitoring stack (emergency only)

### Variables

**GitLab CI/CD Variables**:
- `GRAFANA_ADMIN_PASSWORD` - Admin password
- `ALERTMANAGER_SMTP_PASSWORD` - Email password
- `SLACK_WEBHOOK_URL` - Slack notifications
- `PROMETHEUS_RETENTION_DAYS` - Data retention (default: 15)

## Data Flow

```
┌─────────────┐     Scrape       ┌──────────────┐
│ Exporters   │ ───────────────> │ Prometheus   │
│ - Node      │     /metrics     │ - Store      │
│ - Kube      │                  │ - Alert      │
│ - cAdvisor  │                  └──────────────┘
│ - App       │                           │
└─────────────┘                           │
                                          │ Process
                                          ▼
                                ┌──────────────────┐
                                │ Alertmanager     │
                                │ - Evaluate       │
                                │ - Route          │
                                │ - Notify         │
                                └──────────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
                    ▼                     ▼                     ▼
              ┌──────────┐         ┌─────────┐          ┌──────────┐
              │ Email    │         │  Slack  │          │    SMS   │
              └──────────┘         └─────────┘          └──────────┘
                                          │
                                          │ Query
                                          ▼
                                ┌──────────────────┐
                                │ Grafana          │
                                │ - Dashboards     │
                                │ - Query          │
                                │ - Visualize      │
                                └──────────────────┘
```

## Storage Requirements

### Prometheus TSDB
- **Size**: 50Gi
- **Retention**: 15 days
- **Storage Class**: longhorn
- **Access Mode**: ReadWriteOnce

### Grafana Database
- **Size**: 10Gi
- **Content**: Dashboards, users, data sources
- **Storage Class**: longhorn
- **Access Mode**: ReadWriteOnce

## Security

### Authentication
- **Grafana**: Admin/guest users with passwords
- **Prometheus**: No auth (internal cluster only)
- **Alertmanager**: No auth (internal cluster only)

### Network Policies
- Monitoring namespace can scrape all namespaces
- Grafana accessible via Ingress with TLS
- Prometheus only accessible internally

### Secrets
- Grafana admin password
- Alertmanager SMTP credentials
- Slack webhook URL
- Custom TLS certificates

## Scalability

### Current Setup
- 1 Prometheus replica (HA possible with Thanos)
- 1 Grafana instance (can scale horizontally)
- 1 Alertmanager replica (HA cluster possible)

### Future Enhancements
- Thanos for long-term storage
- Loki for log aggregation
- Tempo for distributed tracing
- Multiple Prometheus replicas (sharding)

## Monitoring Coverage

### Infrastructure (100%)
- All nodes: Node Exporter
- All pods: cAdvisor
- K8s resources: Kube-State-Metrics

### Application (100%)
- Backend: Spring Boot Actuator
- Database: PostgreSQL metrics
- Future: Frontend error tracking

### Alerting (100%)
- Infrastructure alerts
- Application alerts
- Custom business metrics alerts

## Next Steps

1. Deploy monitoring stack using Ansible
2. Configure GitLab CI/CD pipeline
3. Import Grafana dashboards
4. Test alert delivery
5. Tune retention and performance
6. Document runbooks for alerts
7. Train team on dashboards

# OpenTelemetry Implementation Summary

## Overview

This document summarizes all changes made to add OpenTelemetry distributed tracing with Tempo backend to the Ajasta application.

## What Was Implemented

### 1. Backend Code Changes

#### OpenTelemetry Dependencies Added
- **File:** `ajasta-backend/pom.xml`
- **Changes:**
  - Added `opentelemetry-spring-boot-starter` (v2.10.0)
  - Added `opentelemetry-exporter-otlp` (v1.45.0)
  - Added `micrometer-tracing-bridge-otel`
  - Added `opentelemetry-api` and `opentelemetry-instrumentation-annotations`

#### OpenTelemetry Configuration
- **File:** `ajasta-backend/src/main/java/top/ajasta/AjastaApp/config/OpenTelemetryConfig.java`
- **Purpose:** Configures OTLP exporter to send traces to Tempo
- **Features:**
  - OTLP gRPC exporter configuration
  - Service name and resource attributes
  - Batch span processor
  - S3 storage backend support
  - Custom headers for authentication

#### Tracing Utilities
- **File:** `ajasta-backend/src/main/java/top/ajasta/AjastaApp/config/TracingUtils.java`
- **Purpose:** Helper class for manual instrumentation
- **Methods:**
  - `traceRunnable()` - Trace void operations
  - `traceCallable()` - Trace operations with return values
  - `setAttribute()` - Add custom attributes to spans
  - `recordException()` - Record exceptions in traces
  - `addEvent()` - Add events to spans
  - `getCurrentTraceId()` - Get current trace ID for logging

#### Example Instrumentation
- **File:** `ajasta-backend/src/main/java/top/ajasta/AjastaApp/config/InstrumentedControllerExample.java`
- **Purpose:** Demonstrates various instrumentation patterns
- **Examples Include:**
  - HTTP request tracing
  - Error handling and exception recording
  - Nested spans for complex operations
  - External API call tracing
  - Database operation tracing
  - Batch processing with metrics

#### Application Properties
- **File:** `ajasta-backend/src/main/resources/application.properties`
- **Changes:** Added OpenTelemetry configuration section
- **Configuration:**
  - OTLP endpoint
  - Service name
  - Sampler configuration
  - Batch span processor settings
  - Micrometer tracing integration

### 2. Ansible Deployment Changes

#### Tempo Deployment Playbook
- **File:** `ansible/k8s/23-deploy-tempo-kubectl.yml`
- **Purpose:** Deploys Tempo with S3 storage backend
- **Components:**
  - Tempo distributor (OTLP receiver on port 4317)
  - Tempo ingester
  - Tempo query (Web UI on port 16686)
  - S3 storage backend
  - S3 credentials secret
  - Services and Ingress
- **Features:**
  - Configurable S3 bucket and region
  - Persistent storage for WAL and blocks
  - Longhorn storage class
  - Health checks and rollout verification
  - Complete rollback support

#### Tempo Rollback Playbook
- **File:** `ansible/k8s/24-rollback-tempo-kubectl.yml`
- **Purpose:** Removes all Tempo components
- **Cleanup:**
  - Deployments, Services, Ingress
  - ConfigMaps and Secrets
  - PVCs
  - Preserves S3 traces

#### Backend Configuration Updates
- **File:** `ansible/collections/ajasta/app/roles/ajasta_backend/defaults/main.yml`
- **Changes:**
  - Added `opentelemetry_enabled: true`
  - Added `otel_exporter_otlp_endpoint`
  - Added `otel_service_name`
  - Added `otel_traces_sampler_ratio`
  - Added `otel_traces_exporter_timeout`

- **File:** `ansible/collections/ajasta/app/roles/ajasta_backend/tasks/main.yml`
- **Changes:**
  - Added OpenTelemetry environment variables to deployment
  - Conditionally enabled based on `opentelemetry_enabled`

### 3. Documentation

#### Complete Setup Guide
- **File:** `OPENTELEMETRY_SETUP.md`
- **Contents:**
  - Architecture overview
  - Prerequisites and setup instructions
  - Deployment steps
  - Automatic and manual instrumentation examples
  - Tempo UI usage guide
  - Configuration reference
  - Troubleshooting guide
  - Performance considerations

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Ajasta Application                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐          ┌──────────────────────────┐  │
│  │  Spring Boot    │          │   React Frontend         │  │
│  │    Backend      │          │   (Optional: JS tracing) │  │
│  │                 │          │                          │  │
│  │ ┌─────────────┐ │          │                          │  │
│  │ │OpenTelemetry│ │          │                          │  │
│  │ │    SDK      │ │          │                          │  │
│  │ └──────┬──────┘ │          │                          │  │
│  └────────┼────────┘          └──────────────────────────┘  │
│           │                                                 │
│           │ OTLP/gRPC                                       │
└───────────┼─────────────────────────────────────────────────┘
            │
            ▼
┌───────────────────────────────────────────────────────────────┐
│                      Observability Namespace                  │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    Tempo Stack                          │  │
│  │                                                         │  │
│  │  ┌──────────────┐    ┌──────────────┐                   │  │
│  │  │  Distributor │◄───│ OTLP gRPC    │                   │  │
│  │  │   (4317)     │    │   Receiver   │                   │  │
│  │  └──────┬───────┘    └──────────────┘                   │  │
│  │         │                                               │  │
│  │         ▼                                               │  │
│  │  ┌──────────────┐                                       │  │
│  │  │   Ingester   │                                       │  │
│  │  └──────┬───────┘                                       │  │
│  │         │                                               │  │
│  │         ▼                                               │  │
│  │  ┌──────────────┐    ┌──────────────┐                   │  │
│  │  │    Storage   │────│    S3 Bucket │                   │  │
│  │  │  (Parquet)   │    │   (Long-term)│                   │  │
│  │  └──────────────┘    └──────────────┘                   │  │
│  │                                                         │  │
│  │  ┌──────────────┐                                       │  │
│  │  │Tempo Query   │                                       │  │
│  │  │  (Web UI)    │                                       │  │
│  │  │   :16686     │                                       │  │
│  │  └──────────────┘                                       │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

## Usage

### Deploy Tempo

```bash
cd ansible

# Set S3 credentials (if using S3 storage)
export AWS_ACCESS_KEY_ID="your-key"
export AWS_SECRET_ACCESS_KEY="your-secret"
export AWS_REGION="us-east-2"

# Deploy Tempo
ansible-playbook -i k8s/inventory.ini k8s/23-deploy-tempo-kubectl.yml
```

### Deploy Backend with Tracing

```bash
# Rebuild backend image with OpenTelemetry dependencies
cd ajasta-backend
./mvnw clean package
docker build --platform linux/amd64 -t vladimirryrik/ajasta-backend:alpine .
docker push vladimirryrik/ajasta-backend:alpine

# Deploy backend
cd ../ansible
ansible-playbook -i k8s/inventory.ini ajasta-app/22-deploy-backend.yml
```

### Access Tempo UI

```bash
# Port forwarding
kubectl port-forward -n observability svc/tempo-query 16686:16686
# Open: http://localhost:16686

# Or via Ingress
echo "<master-ip> tempo.local" | sudo tee -a /etc/hosts
# Open: http://tempo.local
```

## Instrumentation Examples

### Automatic (No Code Changes)

HTTP requests are automatically traced:
```java
@GetMapping("/api/orders/{id}")
public ResponseEntity<Order> getOrder(@PathVariable Long id) {
    // Automatically traced!
    return ResponseEntity.ok(orderService.findById(id));
}
```

### Manual (Business Logic)

Use `TracingUtils` for custom spans:
```java
@Service
public class OrderService {

    public OrderDTO createOrder(OrderRequest request) {
        return TracingUtils.traceCallable("order.create", () -> {
            TracingUtils.setAttribute("order.total", request.getTotal());
            TracingUtils.setAttribute("order.items", request.getItems().size());

            // Business logic here
            Order order = processOrder(request);

            TracingUtils.addEvent("order_created");
            return convertToDTO(order);
        });
    }
}
```

### Error Handling

```java
try {
    processPayment(request);
    TracingUtils.markAsSuccess();
} catch (PaymentException e) {
    TracingUtils.recordException(e);
    TracingUtils.setAttribute("payment.error", e.getMessage());
    throw e;
}
```

## Environment Variables

### Backend (OpenTelemetry)

| Variable | Default | Description |
|----------|---------|-------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://tempo.observability.svc.cluster.local:4317` | Tempo OTLP endpoint |
| `OTEL_SERVICE_NAME` | `ajasta-backend` | Service name for traces |
| `OTEL_TRACES_SAMPLER_RATIO` | `1.0` | Sampling ratio (1.0 = 100%) |
| `OTEL_TRACES_EXPORTER_TIMEOUT` | `30000` | Export timeout in ms |

### Tempo (S3 Storage)

| Variable | Default | Description |
|----------|---------|-------------|
| `TEMPO_S3_BUCKET` | `ajasta-tempo-traces` | S3 bucket for traces |
| `AWS_REGION` | `us-east-2` | AWS/Yandex Cloud region |
| `AWS_ACCESS_KEY_ID` | - | S3 access key |
| `AWS_SECRET_ACCESS_KEY` | - | S3 secret key |

## Key Features

### ✅ What Works Out of the Box

1. **Automatic HTTP Tracing**
   - All REST endpoints automatically traced
   - HTTP method, path, status code captured
   - Request/response timing measured

2. **Database Tracing**
   - SQL queries automatically traced
   - Connection pool metrics
   - Query execution time

3. **Spring Boot Integration**
   - Seamless integration with Spring Boot 3.x
   - Micrometer tracing bridge
   - Actuator integration

4. **OTLP Protocol**
   - Standard protocol for trace export
   - Works with any OTLP-compatible backend
   - gRPC for efficiency

5. **S3 Storage**
   - Long-term trace persistence
   - Cost-effective storage
   - Scalable to millions of traces

###  Manual Instrumentation

- Custom business logic tracing
- External API call tracking
- Database operation details
- Batch processing metrics
- Event correlation

###  Tempo Features

- **Web UI:** Search and visualize traces
- **Trace ID:** Link traces across services
- **Tags/Attributes:** Filter by custom attributes
- **Duration:** Performance bottlenecks identification
- **Export:** Share traces via URL

## Configuration Files

### Backend Configuration

1. **ajasta-backend/pom.xml** - OpenTelemetry dependencies
2. **ajasta-backend/src/main/resources/application.properties** - OTLP settings
3. **ajasta-backend/src/.../config/OpenTelemetryConfig.java** - Tracer configuration
4. **ajasta-backend/src/.../config/TracingUtils.java** - Helper utilities

### Ansible Configuration

1. **ansible/k8s/23-deploy-tempo-kubectl.yml** - Tempo deployment
2. **ansible/k8s/24-rollback-tempo-kubectl.yml** - Tempo rollback
3. **ansible/collections/.../ajasta_backend/defaults/main.yml** - Backend OTLP vars
4. **ansible/collections/.../ajasta_backend/tasks/main.yml** - Deployment with OTLP env vars

## Next Steps

### Immediate Actions

1. ✅ **Deploy Tempo**
   ```bash
   cd ansible
   ansible-playbook -i k8s/inventory.ini k8s/23-deploy-tempo-kubectl.yml
   ```

2. ✅ **Rebuild Backend**
   ```bash
   cd ajasta-backend
   ./mvnw clean package
   docker build --platform linux/amd64 -t vladimirryrik/ajasta-backend:alpine .
   docker push vladimirryrik/ajasta-backend:alpine
   ```

3. ✅ **Deploy Backend with Tracing**
   ```bash
   cd ansible
   ansible-playbook -i k8s/inventory.ini ajasta-app/22-deploy-backend.yml
   ```

4. ✅ **Generate Traces**
   - Make API calls to backend
   - Access Tempo UI: http://localhost:16686
   - Search for traces by service name

### Future Enhancements

1. **Frontend Tracing**
   - Add OpenTelemetry JS to React frontend
   - Trace user interactions
   - Correlate frontend and backend traces

2. **Additional Services**
   - Instrument PaymentService (Stripe)
   - Instrument NotificationService (Email)
   - Instrument AWSS3Service (File uploads)

3. **Service Maps**
   - Create dependency graph
   - Visualize service communication
   - Identify bottlenecks

4. **Alerting**
   - Set up alerts on error traces
   - Monitor slow database queries
   - Track external API latency

5. **Metrics Integration**
   - Correlate traces with Prometheus metrics
   - Build dashboards in Grafana
   - SLI/SLO tracking

## Troubleshooting

### No Traces in Tempo

1. **Check backend logs:**
   ```bash
   kubectl logs -n ajasta -l app=ajasta-backend --tail=100 | grep -i telemetry
   ```

2. **Check Tempo logs:**
   ```bash
   kubectl logs -n observability -l app=tempo --tail=100
   ```

3. **Verify connectivity:**
   ```bash
   kubectl exec -n ajasta -l app=ajasta-backend -- curl -v http://tempo.observability.svc.cluster.local:4317
   ```

### S3 Connection Issues

1. **Verify credentials:**
   ```bash
   kubectl get secret tempo-s3 -n observability -o yaml
   ```

2. **Test S3 access:**
   ```bash
   aws s3 ls s3://ajasta-tempo-traces
   ```

3. **Check Tempo configuration:**
   ```bash
   kubectl get configmap tempo-config -n observability -o yaml
   ```

## Support and Documentation

- **Setup Guide:** `OPENTELEMETRY_SETUP.md` - Complete documentation
- **Example Code:** `InstrumentedControllerExample.java` - Instrumentation patterns
- **Tempo Docs:** https://grafana.com/docs/tempo/latest/
- **OpenTelemetry Docs:** https://opentelemetry.io/docs/instrumentation/java/

## Summary

This implementation provides:
- ✅ **Automatic tracing** for all HTTP requests and database operations
- ✅ **Manual instrumentation** utilities for custom business logic
- ✅ **Tempo deployment** with S3 storage for trace persistence
- ✅ **Ansible automation** for easy deployment and rollback
- ✅ **Comprehensive documentation** and examples
- ✅ **Production-ready** configuration with resource limits and health checks

You can now:
1. Deploy distributed tracing in a single command
2. Automatically trace all HTTP endpoints without code changes
3. Manually instrument critical business logic
4. Visualize traces in Tempo UI
5. Store traces long-term in S3
6. Troubleshoot performance issues across service boundaries

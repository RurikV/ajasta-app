# OpenTelemetry Distributed Tracing Setup Guide

This guide explains how to use the OpenTelemetry distributed tracing setup that has been integrated into the Ajasta application.

## Overview

The system includes:
- **OpenTelemetry SDK** in the Spring Boot backend for automatic and manual instrumentation
- **Tempo** - Grafana's distributed tracing platform for trace storage and visualization
- **OTLP (OpenTelemetry Protocol)** - For exporting traces to Tempo
- **S3 Storage** - Long-term trace persistence

## Architecture

```
┌─────────────────┐
│ Spring Boot App │
│                 │
│ ┌─────────────┐ │
│ │OpenTelemetry│ │
│ │   SDK       │ │
│ └──────┬──────┘ │
│        │        │
│        │ OTLP   │
│        │ gRPC   │
└────────┼────────┘
         │
         ▼
┌─────────────────┐
│     Tempo       │
│  (Distributor)  │
│                 │
│ ┌─────────────┐ │
│ │  Ingester   │ │
│ └──────┬──────┘ │
│        │        │
│        ▼        │
│ ┌─────────────┐ │
│ │ S3 Storage  │ │
│ └─────────────┘ │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  Tempo Query    │
│   (Web UI)      │
└─────────────────┘
```

## Prerequisites

### 1. S3 Bucket for Trace Storage

Create an S3 bucket (or use your existing one):

```bash
# Using Yandex Cloud Object Storage
yc storage bucket create --name ajasta-tempo-traces

# Or use AWS S3
aws s3 mb s3://ajasta-tempo-traces --region us-east-2
```

### 2. AWS/Yandex Cloud Credentials

Set up your credentials as environment variables:

```bash
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_REGION="us-east-2"
```

## Deployment Steps

### Step 1: Deploy Tempo

Deploy Tempo with S3 storage backend:

```bash
cd ansible

# Deploy Tempo
ansible-playbook -i k8s/inventory.ini k8s/23-deploy-tempo-kubectl.yml
```

This will deploy:
- Tempo distributor (OTLP receiver on port 4317)
- Tempo ingester
- Tempo query (Web UI)
- S3 storage backend
- Ingress for Web UI access

**Expected output:**
```
TEMPO DISTRIBUTED TRACING DEPLOYED SUCCESSFULLY

Access Information:
  Tempo Query UI:
    - Ingress: http://tempo.local
    - Port Forward: kubectl port-forward -n observability svc/tempo-query 16686:16686

  OTLP Endpoints:
    - gRPC: tempo.observability.svc.cluster.local:4317
    - HTTP: tempo.observability.svc.cluster.local:4318
```

### Step 2: Rebuild Backend Docker Image

The backend now includes OpenTelemetry dependencies. Rebuild and push the Docker image:

```bash
cd ajasta-backend

# Build with OpenTelemetry dependencies
./mvnw clean package

# Build Docker image
docker build --platform linux/amd64 -t vladimirryrik/ajasta-backend:alpine .

# Push to registry
docker push vladimirryrik/ajasta-backend:alpine
```

### Step 3: Deploy Backend with Tracing

The backend Ansible role now automatically includes OpenTelemetry environment variables:

```bash
cd ansible

# Deploy backend with tracing enabled
ansible-playbook -i k8s/inventory.ini ajasta-app/22-deploy-backend.yml
```

**OpenTelemetry environment variables automatically configured:**
```yaml
OTEL_EXPORTER_OTLP_ENDPOINT: http://tempo.observability.svc.cluster.local:4317
OTEL_SERVICE_NAME: ajasta-backend
OTEL_TRACES_SAMPLER_RATIO: 1.0
OTEL_TRACES_EXPORTER_TIMEOUT: 30000
```

### Step 4: Verify Tracing

Check that traces are being received:

```bash
# Check Tempo pods
kubectl get pods -n observability

# Check Tempo logs for incoming traces
kubectl logs -n observability -l app=tempo --tail=100 | grep -i trace

# Port forward to access Tempo UI
kubectl port-forward -n observability svc/tempo-query 16686:16686
```

Open browser: http://localhost:16686

## Automatic Instrumentation

The Spring Boot OpenTelemetry starter automatically traces:

### ✅ HTTP Requests
All REST controllers are automatically traced:
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public ResponseEntity<Response<OrderDTO>> getOrderById(@PathVariable Long id) {
        // Automatically traced with span name: "GET /api/orders/{id}"
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}
```

### ✅ Database Operations
JPA/Hibernate queries are automatically traced with SQL details

### ✅ Spring Boot Actuator
Health checks and metrics endpoints are traced

## Manual Instrumentation

For custom business logic, use the provided `TracingUtils` class:

### 1. Add Attributes to Current Span

```java
import top.ajasta.AjastaApp.config.TracingUtils;

@GetMapping("/data/{id}")
public ResponseEntity<Data> getData(@PathVariable Long id) {
    // Add custom attributes to the auto-created HTTP span
    TracingUtils.setAttribute("user.id", id);
    TracingUtils.setAttribute("operation", "getData");

    logger.info("Processing request, TraceID: {}", TracingUtils.getCurrentTraceId());

    return ResponseEntity.ok(dataService.findById(id));
}
```

### 2. Create Custom Spans for Business Logic

```java
@Service
public class OrderService {

    public OrderDTO createOrder(OrderRequest request) {
        return TracingUtils.traceCallable("orderService.createOrder", () -> {
            Span span = Span.current();

            // Add business context
            span.setAttribute("order.total", request.getTotal());
            span.setAttribute("order.items", request.getItems().size());

            // Step 1: Validate
            validateOrder(request);
            TracingUtils.addEvent("order_validated");

            // Step 2: Process payment
            PaymentResult payment = processPayment(request);
            TracingUtils.setAttribute("payment.id", payment.getId());

            // Step 3: Save to database
            Order order = saveOrder(request);
            TracingUtils.addEvent("order_saved");

            return convertToDTO(order);
        });
    }
}
```

### 3. Record Exceptions

```java
@PostMapping("/process")
public ResponseEntity<Result> processRequest(@RequestBody Request request) {
    try {
        if (request.isValid()) {
            TracingUtils.addEvent("validation_passed");
            return ResponseEntity.ok(process(request));
        } else {
            throw new ValidationException("Invalid request");
        }
    } catch (ValidationException e) {
        // Automatically records exception in current span
        TracingUtils.recordException(e);
        TracingUtils.setAttribute("error.type", "validation_error");
        return ResponseEntity.badRequest().body(errorResponse(e));
    }
}
```

### 4. Trace External API Calls

```java
@Service
public class StripePaymentService {

    public Charge createCharge(PaymentRequest request) {
        return TracingUtils.traceCallable("stripe.api.createCharge", () -> {
            Span span = Span.current();

            // Add HTTP attributes
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.url", "https://api.stripe.com/v1/charges");
            span.setAttribute("payment.amount", request.getAmount());

            try {
                Charge charge = stripeClient.createCharge(request);

                span.setAttribute("http.status_code", 200);
                span.setAttribute("stripe.charge.id", charge.getId());
                TracingUtils.markAsSuccess();

                return charge;
            } catch (StripeException e) {
                TracingUtils.recordException(e);
                span.setAttribute("http.status_code", 500);
                throw e;
            }
        });
    }
}
```

### 5. Trace Database Operations

```java
@Repository
public class OrderRepository {

    public Order findByIdWithDetails(Long id) {
        return TracingUtils.traceCallable("database.findOrderById", () -> {
            Span span = Span.current();

            // Add database attributes
            span.setAttribute("db.system", "postgresql");
            span.setAttribute("db.name", "ajastadb");
            span.setAttribute("db.operation", "SELECT");
            span.setAttribute("db.table", "orders");

            return entityManager.createQuery(
                    "SELECT o FROM Order o WHERE o.id = :id", Order.class)
                    .setParameter("id", id)
                    .getSingleResult();
        });
    }
}
```

### 6. Batch Processing with Metrics

```java
@Service
public class BatchProcessingService {

    public BatchResult processBatch(List<Item> items) {
        Span span = tracer.spanBuilder("batch.process").startSpan();

        try (var scope = span.makeCurrent()) {
            span.setAttribute("batch.size", items.size());

            int successCount = 0;
            int failureCount = 0;

            for (Item item : items) {
                try {
                    TracingUtils.traceRunnable("batch.item", () -> {
                        Span itemSpan = Span.current();
                        itemSpan.setAttribute("item.id", item.getId());
                        processItem(item);
                        TracingUtils.addEvent("item_processed");
                    });
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    TracingUtils.recordException(e);
                }
            }

            // Add batch metrics
            span.setAttribute("batch.success_count", successCount);
            span.setAttribute("batch.failure_count", failureCount);

            return new BatchResult(items.size(), successCount, failureCount);
        } finally {
            span.end();
        }
    }
}
```

## Using Tempo UI

### Access Tempo Query UI

```bash
# Option 1: Port forwarding
kubectl port-forward -n observability svc/tempo-query 16686:16686
# Open: http://localhost:16686

# Option 2: Via Ingress (add to /etc/hosts)
echo "<master-ip> tempo.local" | sudo tee -a /etc/hosts
# Open: http://tempo.local
```

### Searching for Traces

1. **By Service Name:**
   - Search: `service.name = "ajasta-backend"`

2. **By HTTP Method:**
   - Search: `http.method = "GET"`

3. **By Endpoint:**
   - Search: `http.target = "/api/orders/123"`

4. **By Custom Attributes:**
   - Search: `user.id = "456"`
   - Search: `order.total > 100`

5. **By Trace ID:**
   - Get trace ID from logs: `TraceID: abc123def456...`
   - Paste in Tempo search

### Example: End-to-End Trace Flow

```
┌──────────────────────────────────────────────────────────┐
│ GET /api/orders/123                                      │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ├─ HTTP GET /api/orders/123                             │
│  │   └─ Controller.getOrderById                          │
│  │       └─ OrderService.getOrderById                    │
│  │           ├─ Database Query                           │
│  │           └─ UserService.getUser                      │
│  │               └─ Database Query                       │
│  │                                                       │
│  └─ Span Attributes:                                     │
│      - service.name: ajasta-backend                      │
│      - http.method: GET                                  │
│      - http.status_code: 200                             │
│      - user.id: 123                                      │
│      - db.system: postgresql                             │
└──────────────────────────────────────────────────────────┘
```

## Configuration

### Backend Configuration

Edit `ajasta-backend/src/main/resources/application.properties`:

```properties
## OPENTELEMETRY CONFIGURATION
# OTLP Exporter endpoint (Tempo)
otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://tempo.observability.svc.cluster.local:4317}
otel.exporter.otlp.protocol=grpc
otel.traces.exporter=otlp

# Service name for tracing
otel.service.name=${OTEL_SERVICE_NAME:ajasta-backend}

# Optional authentication headers for Tempo
otel.exporter.otlp.headers=${OTEL_EXPORTER_OTLP_HEADERS:}

# Sampler configuration (1.0 = sample all traces)
otel.traces.sampler=parentbased_always_on
otel.traces.sampler.ratio=${OTEL_TRACES_SAMPLER_RATIO:1.0}

# Exporter timeout (30 seconds)
otel.traces.exporter.timeout=${OTEL_TRACES_EXPORTER_TIMEOUT:30000}
```

### Tempo Configuration

Edit S3 storage in `ansible/k8s/23-deploy-tempo-kubectl.yml`:

```yaml
tempo_storage_s3_bucket: "ajasta-tempo-traces"
tempo_storage_s3_region: "us-east-2"
tempo_retention_days: 30
```

## Troubleshooting

### 1. No Traces Appearing in Tempo

**Check backend is sending traces:**
```bash
# Check backend logs for OpenTelemetry initialization
kubectl logs -n ajasta -l app=ajasta-backend --tail=100 | grep -i telemetry

# Should see:
# "Configuring OTLP Span Exporter with endpoint: http://tempo.observability.svc.cluster.local:4317"
# "OTLP Span Exporter configured successfully"
```

**Check Tempo is receiving traces:**
```bash
# Check Tempo logs
kubectl logs -n observability -l app=tempo --tail=100 | grep -i "trace received"

# Check Tempo distributor metrics
kubectl port-forward -n observability svc/tempo 3100:3100
curl http://localhost:3100/metrics | grep distributor
```

**Common fixes:**
- Verify OTLP endpoint is correct
- Check network connectivity between namespaces
- Ensure Tempo is ready: `kubectl get pods -n observability`

### 2. S3 Connection Issues

**Verify S3 credentials:**
```bash
# Check secret exists
kubectl get secret tempo-s3 -n observability

# Verify credentials
kubectl get secret tempo-s3 -n observability -o jsonpath='{.data}'
```

**Test S3 connectivity:**
```bash
# From Tempo pod
kubectl exec -n observability -l app=tempo -- env | grep AWS

# Check Tempo logs for S3 errors
kubectl logs -n observability -l app=tempo --tail=100 | grep -i s3
```

### 3. High Memory Usage

Tempo can be memory-intensive. Adjust limits in `ansible/k8s/23-deploy-tempo-kubectl.yml`:

```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 2Gi
```

### 4. Sampling Too Many Traces

Adjust sampling ratio in backend:

```yaml
otel_traces_sampler_ratio: "0.1"  # Sample 10% of traces
```

## Rollback

If you need to remove Tempo:

```bash
cd ansible

# Rollback Tempo
ansible-playbook -i k8s/inventory.ini k8s/24-rollback-tempo-kubectl.yml

# Disable tracing in backend
# Edit: ansible/collections/ajasta/app/roles/ajasta_backend/defaults/main.yml
opentelemetry_enabled: false
```

## Performance Considerations

1. **Sampling:** For high-traffic services, use probability sampling:
   ```properties
   otel.traces.sampler=traceidratio
   otel.traces.sampler.ratio=0.1  # 10% of traces
   ```

2. **Batch Size:** Adjust for better throughput:
   ```properties
   otel.bsp.max.queue.size=2048
   otel.bsp.max.export.batch.size=512
   ```

3. **Retention:** Reduce S3 storage costs:
   ```yaml
   tempo_retention_days: 7  # Keep traces for 7 days only
   ```

## Next Steps

1. **Explore Traces:** Access Tempo UI and explore generated traces
2. **Add Custom Attributes:** Instrument critical business logic
3. **Set Up Alerts:** Configure alerts based on trace data
4. **Correlate with Metrics:** Use Tempo + Prometheus + Grafana for full observability
5. **Distributed Tracing:** Add tracing to frontend (see: https://opentelemetry.io/docs/instrumentation/js/)

## Additional Resources

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [Spring Boot Micrometer Tracing](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observation.micrometer-tracing)
- [OTLP Specification](https://opentelemetry.io/docs/reference/specification/protocol/otlp/)

## Support

For issues or questions:
- Check logs: `kubectl logs -n observability -l app=tempo`
- Check Tempo health: `kubectl port-forward -n observability svc/tempo 3100:3100` → http://localhost:3100/status
- Verify S3 bucket exists and is accessible
- Review OpenTelemetry configuration in application.properties

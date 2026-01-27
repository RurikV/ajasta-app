# OpenTelemetry OTLP Setup with Tempo

This document describes how to configure and use OpenTelemetry with OTLP (OpenTelemetry Protocol) exporter to send distributed traces to Tempo in the Ajasta App.

## Overview

The application now uses:
- **OpenTelemetry SDK** for distributed tracing instrumentation
- **OTLP/gRPC Exporter** for efficient trace transmission to Tempo
- **Spring Boot 3.x** auto-configuration with Micrometer Tracing bridge
- **Tempo** as the distributed tracing backend

## Architecture

```
┌─────────────────┐      OTLP/gRPC      ┌──────────────────┐
│  Ajasta Backend │ ──────────────────> │  Tempo (gRPC)    │
│  (Spring Boot)  │   port 4317         │  Port 4317       │
└─────────────────┘                     └──────────────────┘
       │                                          │
       │ OpenTelemetry API                        │
       │ (Manual Instrumentation)                 │
       └──────────────────────────────────────────┘
                    │
                    ▼
            ┌──────────────┐
            │ TracingUtils │
            └──────────────┘
```

## Configuration

### 1. Maven Dependencies (pom.xml)

The following OpenTelemetry dependencies are included:

```xml
<!-- OpenTelemetry SDK for OTLP Exporter -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.46.0</version>
</dependency>

<!-- OpenTelemetry OTLP Exporter -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
    <version>1.46.0</version>
</dependency>

<!-- OpenTelemetry API -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.46.0</version>
</dependency>

<!-- OpenTelemetry Instrumentation Annotations -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-instrumentation-annotations</artifactId>
    <version>2.10.0</version>
</dependency>

<!-- Micrometer Tracing with OpenTelemetry Bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OpenTelemetry Spring Boot Auto-Configuration -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-autoconfigure</artifactId>
    <version>2.10.0</version>
</dependency>
```

### 2. Application Configuration (application.properties)

```properties
## OPENTELEMETRY TRACING CONFIGURATION FOR TEMPO

# Enable Spring Boot Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Enable Spring Boot Actuator tracing
management.tracing.enabled=true

# Sampling probability (1.0 = 100% of traces)
management.tracing.sampling.probability=${TRACES_SAMPLER_RATIO:1.0}

# OTLP Exporter Configuration for Tempo (Spring Boot 3.x format)
# Use gRPC for better performance
management.otlp.tracing.endpoint=http://tempo.observability.svc.cluster.local:4317
management.otlp.tracing.headers=

# Alternative: Use HTTP/JSON instead of gRPC
# management.otlp.tracing.endpoint=http://tempo.observability.svc.cluster.local:4318/v1/traces

# Service name for traces
spring.application.name=${SERVICE_NAME:ajasta-backend}

# Logging for debugging
logging.level.io.micrometer.tracing=DEBUG
logging.level.io.opentelemetry=DEBUG
```

## Manual Instrumentation

The `TracingUtils` class provides helper methods for manual instrumentation:

### Basic Usage

```java
import top.ajasta.AjastaApp.config.TracingUtils;

// Add attributes to current span
TracingUtils.setAttribute("userId", userId);
TracingUtils.setAttribute("operation", "processPayment");

// Record exceptions
try {
    // business logic
} catch (Exception e) {
    TracingUtils.recordException(e);
    throw e;
}

// Execute code with a new span
TracingUtils.traceRunnable("database.saveUser", () -> {
    userRepository.save(user);
});

// Execute code with a return value
User user = TracingUtils.traceCallable("database.findUser", () -> {
    return userRepository.findById(userId);
});

// Add events
TracingUtils.addEvent("validation_passed");
TracingUtils.addEvent("payment_processed", Map.of("amount", 100.0));
```

### Example Controller

See `InstrumentedControllerExample.java` for comprehensive examples:

```java
@RestController
@RequestMapping("/api/example")
public class InstrumentedControllerExample {

    @Autowired
    private Tracer tracer;

    @GetMapping("/data/{id}")
    public Map<String, Object> getDataById(@PathVariable Long id) {
        // Add custom attributes
        TracingUtils.setAttribute("user.id", id);
        TracingUtils.setAttribute("operation", "getDataById");

        logger.info("Fetching data for ID: {}, TraceID: {}",
            id, TracingUtils.getCurrentTraceId());

        return Map.of("id", id, "data", "example data");
    }

    @PostMapping("/data")
    public Map<String, Object> createData(@RequestBody Map<String, Object> payload) {
        try {
            TracingUtils.addEvent("validation_passed");
            TracingUtils.traceRunnable("database.saveData", () -> {
                // Database operation
            });
            return Map.of("message", "Data created successfully");
        } catch (IllegalArgumentException e) {
            TracingUtils.recordException(e);
            return Map.of("error", e.getMessage());
        }
    }
}
```

## Tempo Deployment

### Deploy Tempo with Ansible

Tempo is already deployed in the Kubernetes cluster with OTLP support:

```bash
# Deploy Tempo (if not already deployed)
ansible-playbook ansible/k8s/23-deploy-tempo-kubectl.yml \
  -i ansible/k8s/inventory.ini
```

### Tempo Endpoints

- **OTLP/gRPC**: `tempo.observability.svc.cluster.local:4317`
- **OTLP/HTTP**: `tempo.observability.svc.cluster.local:4318`
- **Query UI**: `http://tempo.local` (via Ingress)

### Access Tempo Query UI

```bash
# Port forward to access Tempo UI locally
kubectl port-forward -n observability svc/tempo 16686:16686

# Open browser: http://localhost:16686
```

## Trace Visualization

### 1. View Traces in Tempo UI

1. Access Tempo Query UI: `http://tempo.local`
2. Search by:
   - Service Name: `ajasta-backend`
   - Trace ID (from logs)
   - Tags/Attributes
3. Click on a trace to view detailed span information

### 2. View Trace ID in Logs

The application logs trace IDs when you use `TracingUtils.getCurrentTraceId()`:

```java
logger.info("Processing payment, TraceID: {}", TracingUtils.getCurrentTraceId());
```

### 3. Copy Trace ID from Logs

Log output example:
```
Processing payment, TraceID: 4bf92f3577b34da6a3ce929d0e0e4736
```

Use this Trace ID in Tempo UI to find the specific trace.

## Span Attributes

OpenTelemetry automatically adds these attributes to spans:

### HTTP Request Attributes
- `http.method`: GET, POST, etc.
- `http.url`: Request URL
- `http.status_code`: Response status
- `http.route`: Spring MVC route pattern

### Custom Attributes
You can add custom attributes using `TracingUtils.setAttribute()`:

```java
TracingUtils.setAttribute("user.id", userId);
TracingUtils.setAttribute("order.id", orderId);
TracingUtils.setAttribute("payment.amount", amount);
```

### Semantic Attributes

Follow OpenTelemetry semantic conventions for common attributes:

- Database operations: `db.system`, `db.name`, `db.operation`, `db.table`
- HTTP calls: `http.method`, `http.url`, `http.scheme`
- Error handling: `error.type`, `error.message`

Example:

```java
TracingUtils.traceCallable("database.query", () -> {
    Span span = Span.current();
    span.setAttribute("db.system", "postgresql");
    span.setAttribute("db.name", "ajastadb");
    span.setAttribute("db.operation", "SELECT");
    span.setAttribute("db.table", "users");
    return userRepository.findById(userId);
});
```

## Testing

### 1. Verify Tracing is Enabled

Check application logs for OpenTelemetry initialization:

```bash
kubectl logs -f deployment/ajasta-backend -n ajasta
```

Look for:
```
DEBUG io.micrometer.tracing... - Tracing enabled
DEBUG io.opentelemetry... - OpenTelemetry configured
```

### 2. Generate Test Traces

Make API requests to the backend:

```bash
# Generate a trace
curl http://<backend-service>/api/resources

# Check logs for Trace ID
kubectl logs deployment/ajasta-backend -n ajasta | grep TraceID
```

### 3. View Traces in Tempo

1. Open Tempo UI: `http://tempo.local`
2. Search by service name: `ajasta-backend`
3. View traces and spans

## Troubleshooting

### Issue: No traces appearing in Tempo

**Check 1: Tempo is running**
```bash
kubectl get pods -n observability -l app=tempo
kubectl get svc -n observability -l app=tempo
```

**Check 2: Backend can reach Tempo**
```bash
# Test from backend pod
kubectl exec -it deployment/ajasta-backend -n ajasta -- \
  nc -zv tempo.observability.svc.cluster.local 4317
```

**Check 3: OTLP endpoint configuration**
```bash
# Check backend configuration
kubectl exec -it deployment/ajasta-backend -n ajasta -- \
  env | grep OTLP
```

**Check 4: Sampling probability**
- Ensure `management.tracing.sampling.probability` is not 0.0
- Recommended: `1.0` for development, `0.1` for production

### Issue: Trace context not propagated

**Solution: Ensure the tracer bean is injected**

```java
@Autowired
private Tracer tracer;  // Should be available via Spring Boot
```

### Issue: High memory usage

**Solution: Adjust batch span processor settings**

```properties
# In application.properties
otel.bsp.schedule.delay.millis=5000      # Export frequency
otel.bsp.max.queue.size=2048             # Max queue size
otel.bsp.max.export.batch.size=512       # Max batch size
```

## Performance Considerations

### Sampling

In production, use sampling to reduce overhead:

```properties
management.tracing.sampling.probability=0.1  # Sample 10% of traces
```

### Batching

Traces are batched and exported every 5 seconds by default. Adjust this based on your needs:

```properties
# Faster exports (more real-time)
otel.bsp.schedule.delay.millis=1000

# Slower exports (more batching)
otel.bsp.schedule.delay.millis=10000
```

### Compression

OTLP/gRPC uses compression by default to reduce network bandwidth.

## Migration from Zipkin to OTLP

If you were previously using Zipkin exporter:

### Before (Zipkin)
```properties
management.zipkin.tracing.endpoint=http://tempo.observability.svc.cluster.local:9411/api/v2/spans
```

### After (OTLP/gRPC)
```properties
management.otlp.tracing.endpoint=http://tempo.observability.svc.cluster.local:4317
```

**Benefits of OTLP:**
- More efficient binary protocol (Protocol Buffers)
- Better support for span attributes and events
- Standardized format across all OpenTelemetry tools
- gRPC streaming for better performance

## References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [Spring Boot Micrometer Tracing](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability.tracing)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/)

## Summary

The Ajasta App has full distributed tracing support with:

✅ OpenTelemetry SDK with OTLP/gRPC exporter
✅ Spring Boot 3.x auto-configuration
✅ Manual instrumentation utilities (`TracingUtils`)
✅ Tempo as the trace storage backend
✅ OTLP endpoints for efficient trace transmission
✅ Query UI for trace visualization

This setup provides production-ready distributed tracing with minimal overhead and maximum observability.

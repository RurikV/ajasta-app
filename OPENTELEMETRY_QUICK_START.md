# OpenTelemetry Quick Start Guide

## TL;DR - Just Get Me Started!

### 1. Deploy Tempo 

```bash
cd ansible

# Optional: Set S3 credentials if using S3 storage
export AWS_ACCESS_KEY_ID="your-key"
export AWS_SECRET_ACCESS_KEY="your-secret"

# Deploy Tempo
ansible-playbook -i k8s/inventory.ini k8s/23-deploy-tempo-kubectl.yml
```

### 2. Rebuild Backend with Tracing 

```bash
cd ajasta-backend

# Add dependencies to pom.xml 
./mvnw clean package

# Build Docker image
docker build --platform linux/amd64 -t vladimirryrik/ajasta-backend:alpine .

# Push to registry
docker push vladimirryrik/ajasta-backend:alpine
```

### 3. Deploy Backend 

```bash
cd ansible

# Deploy with tracing enabled (automatic!)
ansible-playbook -i k8s/inventory.ini ajasta-app/22-deploy-backend.yml
```

### 4. Access Tempo UI (

```bash
# Port forwarding
kubectl port-forward -n observability svc/tempo 3100:3100

# Open browser
open http://localhost:3100
```

### 5. Generate Traces 

```bash
# Make some API calls
curl http://api.ajasta.top/api/orders/1
curl http://api.ajasta.top/api/resources

# Check Tempo UI - traces appear in seconds!
```

---

## Key Files Modified

### Backend
- ✅ `ajasta-backend/pom.xml` - OpenTelemetry dependencies added
- ✅ `ajasta-backend/src/main/resources/application.properties` - OTLP config added
- ✅ `ajasta-backend/src/.../config/OpenTelemetryConfig.java` - Tracer configuration
- ✅ `ajasta-backend/src/.../config/TracingUtils.java` - Helper utilities

### Ansible
- ✅ `ansible/k8s/23-deploy-tempo-kubectl.yml` - Tempo deployment
- ✅ `ansible/k8s/24-rollback-tempo-kubectl.yml` - Tempo rollback
- ✅ `ansible/.../ajasta_backend/defaults/main.yml` - OTLP environment variables
- ✅ `ansible/.../ajasta_backend/tasks/main.yml` - Deployment with tracing

### Documentation
- ✅ `OPENTELEMETRY_SETUP.md` - Complete guide (400+ lines)
- ✅ `OPENTELEMETRY_CHANGES_SUMMARY.md` - Detailed summary
- ✅ `OPENTELEMETRY_QUICK_START.md` - This file

---

## What You Get

### Automatic Tracing (Zero Code Changes)

All HTTP endpoints are automatically traced:
```java
@RestController
public class OrderController {
    @GetMapping("/api/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        // ✅ Automatically traced!
        return ResponseEntity.ok(orderService.findById(id));
    }
}
```

### Manual Tracing (When You Need It)

```java
import top.ajasta.AjastaApp.config.TracingUtils;

public Order createOrder(OrderRequest request) {
    return TracingUtils.traceCallable("order.create", () -> {
        // Add custom attributes
        TracingUtils.setAttribute("order.total", request.getTotal());

        // Your business logic
        Order order = processOrder(request);

        // Add events
        TracingUtils.addEvent("order_created");

        return order;
    });
}
```

---

## Tempo UI Tips

### Search Traces

```
# By service name
service.name = "ajasta-backend"

# By HTTP method
http.method = "GET"

# By endpoint
http.target = "/api/orders/123"

# By custom attributes
user.id = "456"
```

### Trace View

Shows:
- **Timeline:** Request duration across services
- **Spans:** Individual operations
- **Attributes:** Request metadata
- **Logs:** Events and errors
- **Tags:** Custom attributes

---

## Common Commands

### Check Status

```bash
# Tempo pods
kubectl get pods -n observability

# Backend pods
kubectl get pods -n ajasta

# Tempo logs
kubectl logs -n observability -l app=tempo --tail=50

# Backend logs (check for tracing errors)
kubectl logs -n ajasta -l app=ajasta-backend --tail=50 | grep -i telemetry
```

### Access Services

```bash
# Tempo UI (port forwarding)
kubectl port-forward -n observability svc/tempo-query 16686:16686

# Tempo UI (via Ingress)
echo "<master-ip> tempo.local" | sudo tee -a /etc/hosts
# Open: http://tempo.local

# Tempo distributor (OTLP receiver)
kubectl port-forward -n observability svc/tempo 4317:4317
```

### Troubleshooting

```bash
# No traces? Check backend is configured
kubectl exec -n ajasta -l app=ajasta-backend -- env | grep OTEL

# Check Tempo is receiving
kubectl logs -n observability -l app=tempo --tail=100 | grep -i "trace"

# Test connectivity
kubectl exec -n ajasta -l app=ajasta-backend -- curl -v http://tempo.observability.svc.cluster.local:4317

# Check S3 connection
kubectl logs -n observability -l app=tempo --tail=100 | grep -i s3
```

---

## Rollback

If something goes wrong:

```bash
cd ansible

# Remove Tempo
ansible-playbook -i k8s/inventory.ini k8s/24-rollback-tempo-kubectl.yml

# Disable tracing in backend
# Edit: ansible/collections/ajasta/app/roles/ajasta_backend/defaults/main.yml
# Set: opentelemetry_enabled: false

# Redeploy backend
ansible-playbook -i k8s/inventory.ini ajasta-app/22-deploy-backend.yml
```

---

## Environment Variables

### Backend (Automatic)

Set via Ansible, but can override:

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo.observability.svc.cluster.local:4317
OTEL_SERVICE_NAME=ajasta-backend
OTEL_TRACES_SAMPLER_RATIO=1.0
OTEL_TRACES_EXPORTER_TIMEOUT=30000
```

### Tempo (S3 Storage)

```bash
AWS_ACCESS_KEY_ID=your-key
AWS_SECRET_ACCESS_KEY=your-secret
AWS_REGION=us-east-2
TEMPO_S3_BUCKET=ajasta-tempo-traces
```

---

## What Gets Traced Automatically

### ✅ HTTP Requests
- All REST endpoints
- HTTP method, path, status code
- Request duration

### ✅ Database Operations
- SQL queries
- Connection pool metrics
- Query execution time

### ✅ Spring Boot Components
- Controllers
- Services
- Repositories
- Actuator endpoints

---

## Best Practices

### 1. Add Custom Attributes

```java
@GetMapping("/orders/{id}")
public ResponseEntity<Order> getOrder(@PathVariable Long id) {
    // Add business context
    TracingUtils.setAttribute("user.id", getCurrentUserId());
    TracingUtils.setAttribute("order.id", id);

    return ResponseEntity.ok(orderService.findById(id));
}
```

### 2. Record Exceptions

```java
try {
    processPayment(request);
} catch (PaymentException e) {
    TracingUtils.recordException(e);
    TracingUtils.setAttribute("payment.error", e.getMessage());
    throw e;
}
```

### 3. Create Custom Spans

```java
public void processBatch(List<Item> items) {
    TracingUtils.traceRunnable("batch.process", () -> {
        for (Item item : items) {
            processItem(item);
        }
    });
}
```

### 4. Log Trace IDs

```java
@GetMapping("/orders/{id}")
public ResponseEntity<Order> getOrder(@PathVariable Long id) {
    logger.info("Processing order {}, TraceID: {}", id, TracingUtils.getCurrentTraceId());
    return ResponseEntity.ok(orderService.findById(id));
}
```

---

## Performance Tips

### High Traffic? Reduce Sampling

```yaml
# In backend defaults
otel_traces_sampler_ratio: "0.1"  # Sample 10% instead of 100%
```

### Reduce S3 Costs

```yaml
# In Tempo playbook
tempo_retention_days: 7  # Keep traces 7 days instead of 30
```

### Memory Constraints

```yaml
# In Tempo deployment
resources:
  limits:
    memory: 512Mi  # Reduce from default
```

---

## Next Steps

1. ✅ Deploy Tempo and verify it works
2. ✅ Rebuild backend with tracing
3. ✅ Make API calls and see traces in Tempo UI
4. ✅ Add custom attributes to key endpoints
5. ✅ Set up alerts on error traces
6. ✅ Integrate with Grafana dashboards
7. ✅ Add frontend tracing (OpenTelemetry JS)

---

## Documentation

- **Complete Guide:** `OPENTELEMETRY_SETUP.md`
- **Changes Summary:** `OPENTELEMETRY_CHANGES_SUMMARY.md`
- **Quick Start:** `OPENTELEMETRY_QUICK_START.md` (this file)

---

## Support

Check logs:
```bash
# Tempo
kubectl logs -n observability -l app=tempo --tail=100 -f

# Backend
kubectl logs -n ajasta -l app=ajasta-backend --tail=100 -f | grep -i trace
```

Access Tempo health:
```bash
kubectl port-forward -n observability svc/tempo 3100:3100
curl http://localhost:3100/status
```

---

## TL;DR Summary

1. Deploy Tempo: `ansible-playbook -i k8s/inventory.ini k8s/23-deploy-tempo-kubectl.yml`
2. Rebuild backend: `docker build -t vladimirryrik/ajasta-backend:alpine .`
3. Deploy backend: `ansible-playbook -i k8s/inventory.ini ajasta-app/22-deploy-backend.yml`
4. Access UI: `kubectl port-forward -n observability svc/tempo-query 16686:16686`
5. Open browser: http://localhost:16686
6. Make API calls, see traces.

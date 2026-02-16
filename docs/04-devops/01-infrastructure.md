# DevOps

## How We Run This Thing

Code is only part of the story. Someone has to deploy it, monitor it, fix it when it breaks. That's what this document is about.

---

## Local Development

### What You Need

- Docker Desktop (or Docker Engine + Docker Compose)
- 8GB RAM minimum (16GB recommended)
- 20GB disk space

### Getting Started

```bash
# Clone the repo
git clone <repo-url>
cd ajasta-app

# Start everything
docker compose up -d

# Check it's working
docker compose ps
```

### What Runs

| Service | Port | What it is |
|---------|------|------------|
| Frontend | 3000 | React app |
| Backend | 8090 | Spring Boot API |
| PostgreSQL | 15432 | Database |
| Adminer | 8080 | Database GUI |
| Mailhog | 8025 | Email testing |

### Full Stack (with monitoring)

```bash
# Start everything including OpenSearch, Grafana, Keycloak
docker compose --profile monitoring up -d
```

This adds:

| Service | Port | What it is |
|---------|------|------------|
| OpenSearch | 9200 | Log storage |
| OpenSearch Dashboards | 5601 | Log visualization |
| Keycloak | 8180 | Identity provider |
| Grafana | 3001 | Metrics dashboards |
| Prometheus | 9090 | Metrics collection |
| Tempo | 4317/4318 | Distributed tracing |

---

## Production

### Where We Deploy

Kubernetes cluster with:
- 1 master node
- 3+ worker nodes
- Longhorn for persistent storage
- NGINX Ingress Controller

### Deployment Process

```bash
# Deploy everything
ansible-playbook k8s/deploy-ajasta.yml -i k8s/inventory.ini

# Just the backend (after a code change)
ansible-playbook k8s/deploy-backend.yml -i k8s/inventory.ini

# Check what's running
kubectl get pods -n ajasta
```

### How Updates Work

1. New code merged to main
2. CI builds Docker image
3. CI pushes to registry
4. Ansible updates Kubernetes deployment
5. Rolling update — no downtime
6. Health checks verify new pods are healthy
7. Old pods terminated

If something goes wrong:

```bash
# Rollback to previous version
kubectl rollout undo deployment/ajasta-backend -n ajasta
```

---

## Monitoring

### What We Monitor

**Application metrics:**
- Request rate
- Response time (p50, p95, p99)
- Error rate
- Active bookings

**Infrastructure metrics:**
- CPU usage
- Memory usage
- Disk I/O
- Network traffic

**Business metrics:**
- Bookings per hour
- Revenue
- Active users

### Where to Look

- **Grafana** (port 3001) — Dashboards
- **Prometheus** (port 9090) — Raw metrics
- **OpenSearch Dashboards** (port 5601) — Logs

### Alerts

When things break, we need to know.

| Alert | Threshold | What to do |
|-------|-----------|------------|
| High error rate | > 5% errors | Check logs, recent deploys |
| Slow responses | p95 > 2s | Check DB queries, scale if needed |
| High CPU | > 80% | Scale horizontally |
| Memory pressure | > 85% | Check for leaks, scale |
| Pod crashes | > 3 restarts | Check logs, rollback if recent deploy |

---

## Logging

### How Logs Work

```
Application → Fluent Bit → OpenSearch → Dashboards
```

Applications log to stdout/stderr. Fluent Bit collects and ships to OpenSearch. Dashboards lets us search and visualize.

### Log Format

We use structured JSON:

```json
{
  "timestamp": "2024-02-15T10:30:00Z",
  "level": "INFO",
  "logger": "BookingService",
  "message": "Booking created",
  "traceId": "abc123",
  "userId": "user-456",
  "resourceId": "res-789"
}
```

### Useful Queries

Find all errors:
```
level: ERROR
```

Find logs for a specific user:
```
userId: user-456
```

Find logs for a request:
```
traceId: abc123
```

---

## Tracing

### What It's For

When a request spans multiple services, tracing shows the full path. Where did time go? Where did it fail?

### How It Works

```
Request → Backend → Tempo → Grafana
```

Backend sends spans to Tempo via OTLP. Grafana visualizes traces.

### Using Traces

In Grafana:
1. Go to Explore
2. Select Tempo datasource
3. Search by trace ID or service
4. See the full request timeline

---

## Secrets

### What's a Secret

- Database passwords
- JWT signing keys
- Stripe API keys
- AWS credentials

### How We Handle Them

**Local development:**
- Stored in `.env` files (not committed)
- Defaults in `docker-compose.yml`

**Production:**
- Kubernetes Secrets
- Mounted as environment variables

### What NOT to Do

- Never commit secrets to git
- Never log secrets
- Never share secrets in chat/email

---

## Backups

### What We Back Up

- PostgreSQL database (daily)
- Kubernetes manifests (in git)
- Container images (in registry)

### How to Restore

**Database:**
```bash
# Get a backup
kubectl exec -n ajasta postgres-pod -- pg_dump -U admin ajastadb > backup.sql

# Restore
kubectl exec -i -n ajasta postgres-pod -- psql -U admin ajastadb < backup.sql
```

---

## Common Issues

### "Service unavailable"

1. Check pod status: `kubectl get pods -n ajasta`
2. Check logs: `kubectl logs <pod> -n ajasta`
3. Check events: `kubectl describe pod <pod> -n ajasta`

### "Database connection failed"

1. Is PostgreSQL running?
2. Are credentials correct?
3. Is network policy allowing traffic?

### "Payments not working"

1. Check Stripe dashboard for incidents
2. Check webhook logs
3. Verify API keys are correct

### "Slow responses"

1. Check database query times
2. Check for N+1 queries
3. Scale if CPU-bound

---

## Infrastructure as Code

### Terraform

Provisions cloud resources:
- VMs
- Networks
- Load balancers
- Storage

```bash
cd terraform
terraform plan
terraform apply
```

### Ansible

Configures servers and deploys applications:
- Installs packages
- Sets up Kubernetes
- Deploys workloads

```bash
ansible-playbook k8s/deploy-ajasta.yml -i k8s/inventory.ini
```

### Docker Compose

Local development environment:
- All services in one command
- Consistent across developers
- Easy to reset

---

## CI/CD

### The Pipeline

```
Push to main → Build → Test → Package → Deploy → Verify
```

### What CI Does

1. **Build** — Compile code, build Docker images
2. **Test** — Run unit tests, integration tests
3. **Package** — Push images to registry
4. **Deploy** — Update Kubernetes deployment
5. **Verify** — Health checks, smoke tests

### What CD Doesn't Do

- Deploy to production on every commit (staging first)
- Auto-rollback (manual trigger)
- Deploy on weekends (unless emergency)

---

## Runbooks

When things go wrong, follow the runbook.

### Database is slow

1. Check for long-running queries
2. Check for table locks
3. Consider adding indexes
4. Consider read replicas

### Memory leak

1. Get heap dump
2. Analyze with profiler
3. Find the leak
4. Fix and deploy

### Payment failures

1. Check Stripe status page
2. Verify API keys
3. Check webhook endpoint
4. Check error logs

---

## On-Call

When you're on-call:

1. **Acknowledge alerts quickly** — Even if you're still investigating
2. **Communicate** — Update status page if user-facing
3. **Document** — Write up what happened
4. **Fix root cause** — Not just symptoms

### Escalation

1. Check runbook
2. Search logs/traces
3. Ask in team channel
4. Escalate to lead if stuck > 30 min

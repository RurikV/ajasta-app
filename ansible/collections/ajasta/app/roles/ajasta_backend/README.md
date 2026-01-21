# ajasta_backend Ansible Role

[![Galaxy](https://img.shields.io/badge/galaxy-ajasta.ajasta_backend-blue.svg)](https://galaxy.ansible.com/ajasta/ajasta_backend)

An Ansible role to deploy the Ajasta Spring Boot backend application to Kubernetes.

## Requirements

- Ansible >= 2.9
- Kubernetes cluster >= 1.29
- `kubernetes.core` collection installed
- `kubectl` configured with cluster access

## Role Variables

### Application Configuration

```yaml
# Kubernetes namespace
kubernetes_namespace: "ajasta"

# Application image
app_name: "ajasta-backend"
app_image: "vladimirryrik/ajasta-backend:alpine"
app_replicas: 2
```

### Resources

```yaml
app_resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### Service Configuration

```yaml
service_type: "ClusterIP"
service_port: 8090
target_port: 8090
```

### Ingress Configuration

```yaml
ingress_enabled: true
ingress_host: "api.ajasta.top"
ingress_path: "/"
ingress_tls_enabled: true
ingress_tls_secret: "ajasta-backend-tls"
```

### Autoscaling

```yaml
hpa_enabled: true
hpa_min_replicas: 2
hpa_max_replicas: 5
hpa_target_cpu_utilization: 80
hpa_target_memory_utilization: 80
```

## Dependencies

This role depends on:
- `postgresql_cluster` - For database deployment

## Example Playbook

### Basic Deployment

```yaml
---
- hosts: localhost
  gather_facts: false
  roles:
    - role: ajasta.ajasta_backend
      vars:
        kubernetes_namespace: "ajasta"
        app_image: "vladimirryrik/ajasta-backend:alpine"
        app_replicas: 2
```

### Production Deployment with HPA

```yaml
---
- hosts: localhost
  gather_facts: false
  roles:
    - role: ajasta.ajasta_backend
      vars:
        kubernetes_namespace: "ajasta-production"
        app_replicas: 2
        hpa_enabled: true
        hpa_min_replicas: 2
        hpa_max_replicas: 10
        hpa_target_cpu_utilization: 70
        app_resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
```

### Custom Configuration

```yaml
---
- hosts: localhost
  gather_facts: false
  roles:
    - role: ajasta.ajasta_backend
      vars:
        kubernetes_namespace: "ajasta"
        app_image: "myregistry/ajasta-backend:v1.2.3"
        ingress_host: "backend.mycompany.com"
        configmap_data:
          SPRING_PROFILES_ACTIVE: "production"
          SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres:5432/ajasta"
        persistence_enabled: true
        persistence_size: "20Gi"
```

## Deployment Features

This role creates the following Kubernetes resources:

1. **Namespace** - Kubernetes namespace for the application
2. **ConfigMap** - Application configuration
3. **Secret** - Sensitive data (passwords, keys)
4. **Deployment** - Application deployment with health checks
5. **Service** - ClusterIP service for internal access
6. **Ingress** - External access with TLS
7. **PVC** - Persistent volume for file uploads
8. **HPA** - Horizontal Pod Autoscaler
9. **PodDisruptionBudget** - High availability
10. **ServiceAccount** - Pod identity

## Secrets Management

**WARNING**: This role creates placeholder secrets for testing. For production, use external secret management:

### Option 1: External Secrets Operator

```yaml
# Install External Secrets Operator
kubectl apply -f https://raw.githubusercontent.com/external-secrets/external-secrets/main/deploy/kubernetes/manifests/00-namespace.yaml
kubectl apply -f https://raw.githubusercontent.com/external-secrets/external-secrets/main/deploy/kubernetes/releases/latest/v0.9.0/deploy.yaml

# Create SecretStore
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: ajasta
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
```

### Option 2: Sealed Secrets

```bash
# Install Sealed Secrets
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create sealed secret
kubeseal -f my-secret.yaml -o yaml > my-sealed-secret.yaml
kubectl apply -f my-sealed-secret.yaml
```

### Option 3: Manual Secrets

```bash
# Create secret manually
kubectl create secret generic ajasta-backend-secrets \
  --from-literal=SPRING_DATASOURCE_PASSWORD='your-password' \
  --from-literal=JWT_SECRET='your-jwt-secret' \
  --namespace=ajasta
```

## Health Checks

The application includes Spring Boot Actuator health endpoints:

- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/health/startup` - Startup probe

Configure probe settings:

```yaml
liveness_probe_initial_delay: 60
liveness_probe_period: 30
liveness_probe_failure_threshold: 3

readiness_probe_initial_delay: 30
readiness_probe_period: 10
readiness_probe_failure_threshold: 3
```

## Scaling

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment ajasta-backend --replicas=5 -n ajasta

# Check replicas
kubectl get deployment ajasta-backend -n ajasta
```

### Auto Scaling (HPA)

The role can create a HorizontalPodAutoscaler:

```yaml
hpa_enabled: true
hpa_min_replicas: 2
hpa_max_replicas: 5
hpa_target_cpu_utilization: 80
hpa_target_memory_utilization: 80
```

Check HPA status:

```bash
kubectl get hpa -n ajasta
kubectl describe hpa ajasta-backend -n ajasta
```

## Monitoring

### View Logs

```bash
# All pods
kubectl logs -l app=ajasta-backend -n ajasta

# Specific pod
kubectl logs ajasta-backend-7d6f8f5c9d-abc123 -n ajasta

# Follow logs
kubectl logs -f ajasta-backend-7d6f8f5c9d-abc123 -n ajasta

# Previous container logs
kubectl logs ajasta-backend-7d6f8f5c9d-abc123 -n ajasta --previous
```

### View Events

```bash
kubectl get events -n ajasta --sort-by='.lastTimestamp'
kubectl describe pod ajasta-backend-7d6f8f5c9d-abc123 -n ajasta
```

### Port Forward to Local

```bash
kubectl port-forward deployment/ajasta-backend 8090:8090 -n ajasta
curl http://localhost:8090/actuator/health
```

## Troubleshooting

### Check Deployment Status

```bash
kubectl get deployment ajasta-backend -n ajasta
kubectl describe deployment ajasta-backend -n ajasta
```

### Check Pods

```bash
kubectl get pods -l app=ajasta-backend -n ajasta
kubectl describe pod <pod-name> -n ajasta
```

### Common Issues

**Issue**: Pods in CrashLoopBackOff
```bash
# Check logs
kubectl logs <pod-name> -n ajasta

# Common causes:
# - Database connection failed
# - Missing secrets
# - Out of memory
```

**Issue**: Pods stuck in Pending state
```bash
# Check events
kubectl describe pod <pod-name> -n ajasta

# Common causes:
# - Insufficient resources
# - PVC not bound
# - Image pull errors
```

**Issue**: Health checks failing
```bash
# Check actuator endpoints
kubectl port-forward <pod-name> 8090:8090 -n ajasta
curl http://localhost:8090/actuator/health

# Check application logs for errors
kubectl logs <pod-name> -n ajasta
```

## Updates and Rollouts

### Update Image

```yaml
# In playbook
app_image: "vladimirryrik/ajasta-backend:v1.2.0"
```

### Rollout Status

```bash
kubectl rollout status deployment/ajasta-backend -n ajasta
```

### Rollback

```bash
# Check rollout history
kubectl rollout history deployment/ajasta-backend -n ajasta

# Rollback to previous version
kubectl rollout undo deployment/ajasta-backend -n ajasta

# Rollback to specific revision
kubectl rollout undo deployment/ajasta-backend --to-revision=3 -n ajasta
```

## Uninstall

```bash
# Delete deployment
kubectl delete deployment ajasta-backend -n ajasta

# Delete all resources
kubectl delete all -l app=ajasta-backend -n ajasta

# Delete namespace (removes everything)
kubectl delete namespace ajasta
```

## License

MIT

## Author Information

- **Author**: Ajasta DevOps Team
- **Email**: devops@ajasta.top

# Ajasta App Helm Chart

This Helm chart deploys the Ajasta application stack to Kubernetes, including:
- PostgreSQL database (StatefulSet with persistent storage)
- Backend API (Spring Boot application)
- Frontend (React application served via Nginx)
- Ingress controller configuration

## Prerequisites

- Kubernetes cluster 1.19+
- Helm 3.0+
- Persistent storage provider (e.g., Longhorn, Rook-Ceph)
- Ingress controller (e.g., nginx-ingress)

## Installation

### Basic Installation

```bash
helm install ajasta ./helm/ajasta-app --namespace ajasta --create-namespace
```

### Installation with Custom Values

```bash
helm install ajasta ./helm/ajasta-app \
  --namespace ajasta \
  --create-namespace \
  --set backend.image.tag=v1.0.0 \
  --set frontend.image.tag=v1.0.0 \
  --set postgres.auth.password=securepassword \
  --set backend.secrets.jwtSecret=your-jwt-secret
```

### Installation from Values File

```bash
helm install ajasta ./helm/ajasta-app \
  --namespace ajasta \
  --create-namespace \
  --values custom-values.yaml
```

## Configuration

### Key Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `global.namespace` | Kubernetes namespace | `ajasta` |
| `postgres.enabled` | Enable PostgreSQL | `true` |
| `postgres.image.repository` | PostgreSQL image | `postgres` |
| `postgres.image.tag` | PostgreSQL image tag | `16-alpine` |
| `postgres.persistence.size` | Storage size for PostgreSQL | `5Gi` |
| `postgres.persistence.storageClassName` | Storage class name | `longhorn` |
| `postgres.auth.database` | Database name | `ajastadb` |
| `postgres.auth.username` | Database username | `admin` |
| `postgres.auth.password` | Database password | `adminpw` |
| `backend.enabled` | Enable backend API | `true` |
| `backend.image.repository` | Backend image | `vladimirryrik/ajasta-backend` |
| `backend.image.tag` | Backend image tag | `alpine` |
| `backend.replicas` | Number of backend replicas | `1` |
| `backend.secrets.jwtSecret` | JWT secret key | `change-me-production-secret-key` |
| `backend.secrets.awsAccessKeyId` | AWS access key | `""` |
| `backend.secrets.awsSecretAccessKey` | AWS secret key | `""` |
| `backend.config.awsRegion` | AWS region | `us-east-1` |
| `backend.config.awsS3Bucket` | AWS S3 bucket | `""` |
| `frontend.enabled` | Enable frontend | `true` |
| `frontend.image.repository` | Frontend image | `vladimirryrik/ajasta-frontend` |
| `frontend.image.tag` | Frontend image tag | `alpine` |
| `frontend.replicas` | Number of frontend replicas | `1` |
| `ingress.enabled` | Enable ingress | `true` |
| `ingress.className` | Ingress class name | `nginx` |
| `ingress.hosts[0].host` | Ingress hostname | `ajasta.local` |

For complete configuration options, see `values.yaml`.

## Upgrading

```bash
helm upgrade ajasta ./helm/ajasta-app \
  --namespace ajasta \
  --set backend.image.tag=v1.1.0
```

## Uninstalling

```bash
helm uninstall ajasta --namespace ajasta
```

## GitLab CI/CD Integration

### Required CI/CD Variables

Configure these variables in GitLab CI/CD Settings (`Settings > CI/CD > Variables`):

#### Kubernetes Configuration

| Variable | Description | Type | Protected | Masked |
|----------|-------------|------|-----------|--------|
| `KUBECONFIG_CONTENT` | Base64-encoded kubeconfig file | Variable | Yes | Yes |
| `K8S_STAGING_INGRESS_HOST` | Staging ingress hostname | Variable | No | No |
| `K8S_PRODUCTION_INGRESS_HOST` | Production ingress hostname | Variable | No | No |

#### Database Credentials

| Variable | Description | Type | Protected | Masked |
|----------|-------------|------|-----------|--------|
| `POSTGRES_PASSWORD` | PostgreSQL password | Variable | Yes | Yes |

#### Application Secrets

| Variable | Description | Type | Protected | Masked |
|----------|-------------|------|-----------|--------|
| `JWT_SECRET` | JWT secret key for authentication | Variable | Yes | Yes |

#### Optional: AWS Configuration

| Variable | Description | Type | Protected | Masked |
|----------|-------------|------|-----------|--------|
| `AWS_ACCESS_KEY_ID` | AWS access key ID | Variable | Yes | Yes |
| `AWS_SECRET_ACCESS_KEY` | AWS secret access key | Variable | Yes | Yes |
| `AWS_S3_BUCKET` | AWS S3 bucket name | Variable | No | No |

#### Optional: Stripe Configuration

| Variable | Description | Type | Protected | Masked |
|----------|-------------|------|-----------|--------|
| `STRIPE_PUBLIC_KEY` | Stripe public key | Variable | No | No |
| `STRIPE_SECRET_KEY` | Stripe secret key | Variable | Yes | Yes |

### Generating KUBECONFIG_CONTENT

To generate the base64-encoded kubeconfig:

```bash
# From your kubeconfig file
cat ~/.kube/config | base64 | tr -d '\n'

# Or from a specific kubeconfig
cat /path/to/kubeconfig | base64 | tr -d '\n'
```

Copy the output and add it as `KUBECONFIG_CONTENT` variable in GitLab.

### Pipeline Jobs

The GitLab CI/CD pipeline includes:

1. **helm:lint** - Validates Helm chart syntax (runs on all branches)
2. **deploy:k8s:staging** - Deploys to staging environment (develop branch, manual)
3. **deploy:k8s:production** - Deploys to production environment (main/tags, manual)

### Deployment Process

1. Push changes to `develop` or `main` branch
2. Pipeline builds Docker images
3. Navigate to `CI/CD > Pipelines`
4. Click on the pipeline
5. Manually trigger `deploy:k8s:staging` or `deploy:k8s:production`

## Helm Linting

Validate the chart locally:

```bash
helm lint ./helm/ajasta-app
```

Test template rendering:

```bash
helm template ajasta ./helm/ajasta-app --namespace ajasta --debug
```

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -n ajasta
```

### View Pod Logs

```bash
# Backend logs
kubectl logs -f deployment/ajasta-backend -n ajasta

# Frontend logs
kubectl logs -f deployment/ajasta-frontend -n ajasta

# PostgreSQL logs
kubectl logs -f statefulset/ajasta-postgres -n ajasta
```

### Check Ingress

```bash
kubectl get ingress -n ajasta
kubectl describe ingress ajasta-ingress -n ajasta
```

### Check Services

```bash
kubectl get services -n ajasta
```

### Helm Release Status

```bash
helm status ajasta --namespace ajasta
```

### Helm Release History

```bash
helm history ajasta --namespace ajasta
```

## Architecture

```
┌─────────────────────────────────────────────┐
│              Ingress Controller             │
│  (nginx) - Routes traffic by path           │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌──────────────┐ ┌──────────────┐
│   Frontend   │ │   Backend    │
│   Service    │ │   Service    │
│  (ClusterIP) │ │  (ClusterIP) │
└──────┬───────┘ └──────┬───────┘
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│   Frontend   │ │   Backend    │
│  Deployment  │ │  Deployment  │
│   (Nginx)    │ │ (Spring Boot)│
└──────────────┘ └──────┬───────┘
                        │
                        ▼
                 ┌──────────────┐
                 │  PostgreSQL  │
                 │   Service    │
                 │ (Headless)   │
                 └──────┬───────┘
                        │
                        ▼
                 ┌──────────────┐
                 │  PostgreSQL  │
                 │ StatefulSet  │
                 │ + PVC (5Gi)  │
                 └──────────────┘
```

## License

See project LICENSE file.

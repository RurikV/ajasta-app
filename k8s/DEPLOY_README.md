# Ajasta Application Deployment to Kubernetes

This directory contains Ansible playbooks and Kubernetes manifests to deploy the complete Ajasta application stack to a Kubernetes cluster.

## Overview

The deployment includes:
- **PostgreSQL Database**: StatefulSet with persistent storage (5Gi)
- **Backend API**: Spring Boot application (2 replicas)
- **Frontend**: React application with nginx (2 replicas)
- **Ingress**: Nginx Ingress Controller for external access

## Architecture

```
┌─────────────────────────────────────────────────┐
│           Nginx Ingress Controller              │
│                                                  │
│  /api  ──► Backend Service (port 8090)          │
│  /     ──► Frontend Service (port 80)           │
└─────────────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
┌───────▼────────┐    ┌────────▼────────┐
│   Backend      │    │   Frontend      │
│   (2 replicas) │    │   (2 replicas)  │
│   Port: 8090   │    │   Port: 80      │
└───────┬────────┘    └─────────────────┘
        │
        │
┌───────▼────────┐
│   PostgreSQL   │
│   StatefulSet  │
│   Port: 5432   │
└────────────────┘
```

## Directory Structure

```
k8s/
├── deploy-ajasta.yml          # Main deployment playbook
├── manifests/                 # Kubernetes manifests
│   ├── 01-namespace.yml       # Namespace definition
│   ├── 02-postgres-secret.yml # Database credentials
│   ├── 03-postgres-statefulset.yml  # PostgreSQL database
│   ├── 04-postgres-service.yml      # Database service
│   ├── 05-backend-secret.yml        # Backend secrets
│   ├── 06-backend-configmap.yml     # Backend configuration
│   ├── 07-backend-deployment.yml    # Backend deployment
│   ├── 08-backend-service.yml       # Backend service
│   ├── 09-frontend-deployment.yml   # Frontend deployment
│   ├── 10-frontend-service.yml      # Frontend service
│   └── 11-ingress.yml              # Ingress rules
└── DEPLOY_README.md           # This file
```

## Prerequisites

Before deploying the application, ensure:

1. **Kubernetes cluster is running**
   - Use `yc-create.yml` playbook to create the cluster if not already done
   - Cluster should have at least 2 nodes (1 master, 1+ workers)

2. **Ansible inventory is configured**
   - `inventory.ini` file exists with k8s_master group defined
   - SSH access to the master node is configured

3. **kubectl is installed on the master node**
   - The playbook will check for kubectl availability

4. **Docker images are available**
   - Backend: `vladimirryrik/ajasta-backend:alpine`
   - Frontend: `vladimirryrik/ajasta-frontend:alpine`

## Deployment

### Quick Start

Deploy the entire Ajasta application with one command:

```bash
ansible-playbook deploy-ajasta.yml -i inventory.ini
```

### What the Playbook Does

1. **Checks prerequisites**: Verifies kubectl is available on master node
2. **Installs Nginx Ingress Controller**: If not already installed
3. **Creates namespace**: Creates `ajasta` namespace for all resources
4. **Deploys PostgreSQL**: 
   - Creates secrets with database credentials
   - Deploys StatefulSet with persistent storage
   - Creates headless service for database access
   - Waits for database to be ready
5. **Deploys Backend**:
   - Creates secrets (JWT, AWS, Stripe, Mail credentials)
   - Creates ConfigMap (database URL, configuration)
   - Deploys backend with 2 replicas
   - Creates service to expose backend
   - Waits for backend pods to be ready
6. **Deploys Frontend**:
   - Deploys frontend with 2 replicas
   - Creates service to expose frontend
   - Waits for frontend pods to be ready
7. **Configures Ingress**:
   - Creates ingress rules to route traffic
   - Waits for external IP assignment
8. **Reports status**: Displays deployment status and ingress information

### Deployment Time

Typical deployment times:
- Nginx Ingress Controller installation: 2-3 minutes (if needed)
- PostgreSQL ready: 1-2 minutes
- Backend ready: 2-3 minutes
- Frontend ready: 1 minute
- **Total: 6-9 minutes** (first deployment)

Subsequent deployments: 3-5 minutes (ingress already installed)

## Configuration

### Database Credentials

Default credentials are defined in `manifests/02-postgres-secret.yml`:
- Database: `ajastadb`
- Username: `admin`
- Password: `adminpw`

**⚠️ IMPORTANT**: Change these credentials for production deployments!

### Backend Configuration

#### Secrets (manifests/05-backend-secret.yml)

Required:
- `DB_PASSWORD`: Database password (must match PostgreSQL secret)
- `JWT_SECRET`: Secret key for JWT token generation

Optional (for production features):
- `MAIL_USERNAME`: Email service username
- `MAIL_PASSWORD`: Email service password
- `AWS_ACCESS_KEY_ID`: AWS access key for S3
- `AWS_SECRET_ACCESS_KEY`: AWS secret key
- `STRIPE_PUBLIC_KEY`: Stripe public key
- `STRIPE_SECRET_KEY`: Stripe secret key

#### ConfigMap (manifests/06-backend-configmap.yml)

- `DB_URL`: PostgreSQL connection URL
- `DB_USERNAME`: Database username
- `AWS_REGION`: AWS region (default: us-east-1)
- `AWS_S3_BUCKET`: S3 bucket name (optional)
- `JAVA_OPTS`: JVM configuration options

### Resource Limits

Default resource allocations:

**PostgreSQL**:
- Requests: 256Mi RAM, 250m CPU
- Limits: 512Mi RAM, 500m CPU
- Storage: 5Gi persistent volume

**Backend**:
- Requests: 512Mi RAM, 500m CPU
- Limits: 1Gi RAM, 1000m CPU
- Replicas: 2

**Frontend**:
- Requests: 128Mi RAM, 100m CPU
- Limits: 256Mi RAM, 200m CPU
- Replicas: 2

Adjust these in the respective deployment manifests if needed.

## Accessing the Application

After successful deployment:

1. **Get the Ingress IP**:
   ```bash
   kubectl get ingress -n ajasta
   ```

2. **Access the application**:
   - Frontend: `http://<INGRESS_IP>/`
   - Backend API: `http://<INGRESS_IP>/api`

3. **Check deployment status**:
   ```bash
   kubectl get all -n ajasta
   ```

## Monitoring

### Check Pod Status

```bash
kubectl get pods -n ajasta
```

### Check Logs

```bash
# PostgreSQL logs
kubectl logs -n ajasta -l component=database

# Backend logs
kubectl logs -n ajasta -l component=backend

# Frontend logs
kubectl logs -n ajasta -l component=frontend
```

### Check Services

```bash
kubectl get services -n ajasta
```

### Check Ingress

```bash
kubectl describe ingress ajasta-ingress -n ajasta
```

## Troubleshooting

### Pods Not Starting

Check pod events:
```bash
kubectl describe pod <pod-name> -n ajasta
```

### Backend Connection Issues

Verify database is ready:
```bash
kubectl get statefulset -n ajasta
kubectl logs -n ajasta ajasta-postgres-0
```

### Ingress Not Accessible

Check ingress controller:
```bash
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller
```

### Database Persistence Issues

Check PersistentVolumeClaims:
```bash
kubectl get pvc -n ajasta
kubectl describe pvc postgres-data-ajasta-postgres-0 -n ajasta
```

## Updating the Application

### Update Backend/Frontend Images

1. Edit the respective deployment manifest (07 or 09)
2. Change the image tag
3. Re-run the playbook:
   ```bash
   ansible-playbook deploy-ajasta.yml -i inventory.ini
   ```

Or use kubectl directly:
```bash
kubectl set image deployment/ajasta-backend backend=vladimirryrik/ajasta-backend:new-tag -n ajasta
kubectl set image deployment/ajasta-frontend frontend=vladimirryrik/ajasta-frontend:new-tag -n ajasta
```

### Update Configuration

1. Edit ConfigMap or Secret files
2. Re-run the playbook
3. Restart pods to pick up changes:
   ```bash
   kubectl rollout restart deployment/ajasta-backend -n ajasta
   kubectl rollout restart deployment/ajasta-frontend -n ajasta
   ```

## Scaling

### Scale Backend

```bash
kubectl scale deployment/ajasta-backend --replicas=3 -n ajasta
```

### Scale Frontend

```bash
kubectl scale deployment/ajasta-frontend --replicas=3 -n ajasta
```

## Cleanup

### Remove Application (Keep Cluster)

```bash
kubectl delete namespace ajasta
```

### Remove Everything Including Cluster

```bash
# Remove application
kubectl delete namespace ajasta

# Destroy cluster
ansible-playbook yc-destroy.yml -i inventory.ini
```

## Security Considerations

⚠️ **Before production deployment**:

1. **Change all default passwords** in secrets
2. **Generate a strong JWT secret** (at least 32 characters)
3. **Configure TLS/SSL** for ingress (add cert-manager)
4. **Set up proper RBAC** for service accounts
5. **Enable network policies** to restrict pod communication
6. **Use external secret management** (e.g., HashiCorp Vault)
7. **Configure resource quotas** for the namespace
8. **Set up monitoring** (Prometheus, Grafana)
9. **Configure backups** for PostgreSQL data

## Support

For issues or questions:
- Check the main README.md in the project root
- Review Kubernetes cluster setup in k8s/README.md
- Check application logs using the monitoring commands above

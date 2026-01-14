# Ajasta Application Deployment with Ansible

This directory contains Ansible playbooks for deploying the complete Ajasta appointment booking application to Kubernetes.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Deployment Playbooks](#deployment-playbooks)
- [Configuration](#configuration)
- [Accessing the Application](#accessing-the-application)
- [Troubleshooting](#troubleshooting)
- [Maintenance](#maintenance)

## 🎯 Overview

The Ajasta application consists of three main components:

1. **PostgreSQL Database** - High-availability cluster using CloudNativePG operator
2. **Spring Boot Backend** - RESTful API with business logic
3. **React Frontend** - User interface served by Nginx

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Ingress NGINX                          │
│                    (ajasta.local)                            │
└───────────────────────────┬─────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌──────────────────────┐        ┌──────────────────────┐
│   Frontend (Nginx)   │        │   Backend (Spring)   │
│   Port: 80           │────▶   │   Port: 8090         │
│   ajasta-frontend    │        │   ajasta-backend     │
└──────────────────────┘        └──────────┬───────────┘
                                          │
                                          ▼
                                ┌──────────────────────┐
                                │  PostgreSQL Cluster  │
                                │  Port: 5432          │
                                │  cluster-postgresql  │
                                └──────────────────────┘
```

## 📦 Prerequisites

### Required Components

1. **Kubernetes Cluster** - Fully functional K8s cluster with at least 2 worker nodes
2. **NGINX Ingress Controller** - Installed and configured
3. **Longhorn Storage** - For persistent volumes (or alternative StorageClass)
4. **Ansible** - Version 2.12 or higher
5. **SSH Access** - To Kubernetes master node

### Verify Prerequisites

```bash
# Check Kubernetes cluster
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get nodes' --become

# Check Ingress Controller
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get svc -n ingress-nginx' --become

# Check Longhorn Storage
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get storageclass' --become
```

## 🚀 Quick Start

### Complete Deployment (One Command)

Deploy the entire Ajasta application stack:

```bash
cd ansible/ajasta-app
ansible-playbook -i ../k8s/inventory.ini 20-deploy-ajasta-app.yml
```

This will deploy:
1. PostgreSQL HA cluster (2 instances)
2. Backend API (1 replica)
3. Frontend (1 replica)
4. Ingress configuration

### Step-by-Step Deployment

If you prefer to deploy components individually:

```bash
# 1. Deploy PostgreSQL Cluster
ansible-playbook -i ../k8s/inventory.ini 21-deploy-postgres-cluster.yml

# 2. Deploy Backend API
ansible-playbook -i ../k8s/inventory.ini 22-deploy-backend.yml

# 3. Deploy Frontend
ansible-playbook -i ../k8s/inventory.ini 23-deploy-frontend.yml

# 4. Configure Ingress
ansible-playbook -i ../k8s/inventory.ini 24-deploy-ingress.yml

# 5. Verify Deployment
ansible-playbook -i ../k8s/inventory.ini 25-verify-deployment.yml
```

## 📚 Deployment Playbooks

### Master Playbook

#### `20-deploy-ajasta-app.yml`
**Purpose**: Deploy complete Ajasta application stack
**Components**: PostgreSQL + Backend + Frontend + Ingress
**Usage**:
```bash
ansible-playbook -i ../k8s/inventory.ini 20-deploy-ajasta-app.yml
```
**Estimated Time**: 10-15 minutes

### Component Playbooks

#### `21-deploy-postgres-cluster.yml`
**Purpose**: Deploy high-availability PostgreSQL cluster using CloudNativePG
**Details**:
- 2 PostgreSQL instances (1 primary, 1 replica)
- Automatic failover
- Daily backups to S3 (optional)
- Storage: Longhorn 5Gi per instance

**Configuration**:
```yaml
pg_instances: 2
pg_storage_class: longhorn
pg_storage_size: 5Gi
pg_database: ajastadb
```

**Usage**:
```bash
ansible-playbook -i ../k8s/inventory.ini 21-deploy-postgres-cluster.yml
```

#### `22-deploy-backend.yml`
**Purpose**: Deploy Spring Boot Backend API
**Details**:
- Image: `vladimirryrik/ajasta-backend:alpine`
- Port: 8090
- Resources: 500m CPU / 512Mi memory
- Auto-restart on failure

**Configuration**:
```yaml
backend_replicas: 1
backend_image: vladimirryrik/ajasta-backend
backend_tag: alpine
```

**Usage**:
```bash
ansible-playbook -i ../k8s/inventory.ini 22-deploy-backend.yml
```

#### `23-deploy-frontend.yml`
**Purpose**: Deploy React Frontend (Nginx)
**Details**:
- Image: `vladimirryrik/ajasta-frontend:alpine`
- Port: 80
- Resources: 100m CPU / 64Mi memory
- Built-in API proxy to backend

**Configuration**:
```yaml
frontend_replicas: 1
frontend_image: vladimirryrik/ajasta-frontend
frontend_tag: alpine
```

**Usage**:
```bash
ansible-playbook -i ../k8s/inventory.ini 23-deploy-frontend.yml
```

#### `24-deploy-ingress.yml`
**Purpose**: Configure NGINX Ingress for external access
**Details**:
- Host: `ajasta.local`
- Routes: `/` → frontend, `/api` → backend
- TLS support (optional)

**Configuration**:
```yaml
ingress_host: ajasta.local
ingress_class: nginx
tls_enabled: false
```

**Usage**:
```bash
ansible-playbook -i ../k8s/inventory.ini 24-deploy-ingress.yml
```

#### `25-verify-deployment.yml`
**Purpose**: Verify all components are running correctly
**Checks**:
- PostgreSQL cluster health
- Backend API readiness
- Frontend accessibility
- Service connectivity
- Ingress configuration

**Usage**:
```bash
ansible-playbook -i ../k8s/inventory.ini 25-verify-deployment.yml
```

## ⚙️ Configuration

### Environment Variables

Most configuration is done through variables in the playbooks. Here are the key variables you might want to customize:

### PostgreSQL Configuration

Edit `21-deploy-postgres-cluster.yml`:

```yaml
vars:
  pg_instances: 2                    # Number of PostgreSQL instances
  pg_storage_size: 5Gi              # Storage size per instance
  pg_storage_class: longhorn        # Storage class to use
  pg_max_connections: 100           # PostgreSQL max connections
```

### Backend Configuration

Edit `22-deploy-backend.yml`:

```yaml
vars:
  backend_replicas: 1               # Number of backend pods
  backend_image: vladimirryrik/ajasta-backend
  backend_tag: alpine               # Docker image tag
  jwt_secret: change-me-production-secret-key  # CHANGE THIS!
  mail_username: ""                # Optional: SMTP username
  mail_password: ""                # Optional: SMTP password
```

### Frontend Configuration

Edit `23-deploy-frontend.yml`:

```yaml
vars:
  frontend_replicas: 1              # Number of frontend pods
  frontend_image: vladimirryrik/ajasta-frontend
  frontend_tag: alpine              # Docker image tag
```

### Ingress Configuration

Edit `24-deploy-ingress.yml`:

```yaml
vars:
  ingress_host: ajasta.local        # External hostname
  tls_enabled: false                # Enable TLS
```

## 🌐 Accessing the Application

### Option 1: Via Ingress (Recommended)

1. **Get Ingress Controller IP**:
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

2. **Update /etc/hosts** (on your local machine):
```bash
<INGRESS_IP> ajasta.local
```

3. **Access the Application**:
- Frontend: http://ajasta.local
- Backend API: http://ajasta.local/api

### Option 2: Port Forwarding

**Frontend**:
```bash
kubectl port-forward -n ajasta svc/ajasta-frontend 3000:80
# Open http://localhost:3000
```

**Backend API**:
```bash
kubectl port-forward -n ajasta svc/ajasta-backend 8090:8090
# Test: curl http://localhost:8090/api
```

**PostgreSQL**:
```bash
kubectl exec -it -n postgresql-cluster cluster-postgresql-1 -- psql -U app -d cluster-postgresql
```

## 🔧 Troubleshooting

### Check All Components

```bash
# Verify deployment
ansible-playbook -i ../k8s/inventory.ini 25-verify-deployment.yml
```

### Common Issues

#### 1. PostgreSQL Pods Not Ready

**Symptoms**: Pods in Pending or CrashLoopBackOff state

**Solutions**:
```bash
# Check pod status
kubectl get pods -n postgresql-cluster

# Check pod logs
kubectl logs -n postgresql-cluster cluster-postgresql-1

# Check events
kubectl describe pod -n postgresql-cluster cluster-postgresql-1

# Check PVC status
kubectl get pvc -n postgresql-cluster
```

#### 2. Backend Not Connecting to Database

**Symptoms**: Backend logs show connection errors

**Solutions**:
```bash
# Check backend logs
kubectl logs -n ajasta -l component=backend --tail=50

# Verify database service
kubectl get svc -n postgresql-cluster

# Test connectivity from backend pod
kubectl exec -it -n ajasta <backend-pod> -- sh
# Inside pod:
nc -zv cluster-postgresql-rw.postgresql-cluster.svc 5432
```

#### 3. Frontend Not Accessible

**Symptoms**: Cannot access frontend via Ingress or port-forward

**Solutions**:
```bash
# Check frontend pods
kubectl get pods -n ajasta -l component=frontend

# Check frontend logs
kubectl logs -n ajasta -l component=frontend

# Test health endpoint
kubectl exec -n ajasta <frontend-pod> -- curl http://localhost/health
```

#### 4. Ingress Not Working

**Symptoms**: Cannot access application via configured host

**Solutions**:
```bash
# Check ingress resource
kubectl get ingress -n ajasta

# Check ingress controller
kubectl get svc -n ingress-nginx

# Check ingress logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx

# Verify DNS/hosts configuration
nslookup ajasta.local
# or
ping ajasta.local
```

### Resource Issues

#### High Memory/CPU Usage

```bash
# Check pod resource usage
kubectl top pods -n ajasta

# Check node resource usage
kubectl top nodes

# Increase resource limits in playbooks if needed
```

### Storage Issues

#### PVCs Pending or Not Bound

```bash
# Check PVC status
kubectl get pvc -n postgresql-cluster
kubectl get pvc -n ajasta

# Check StorageClass
kubectl get storageclass

# Check Longhorn volumes
kubectl get volumes -n longhorn-system
```

## 🔍 Monitoring and Logs

### View Logs

**Backend logs**:
```bash
# Real-time logs
kubectl logs -n ajasta -l component=backend -f

# Last 100 lines
kubectl logs -n ajasta -l component=backend --tail=100

# Logs from specific pod
kubectl logs -n ajasta <pod-name> --tail=50
```

**Frontend logs**:
```bash
kubectl logs -n ajasta -l component=frontend -f
```

**PostgreSQL logs**:
```bash
kubectl logs -n postgresql-cluster cluster-postgresql-1 -f
```

### Monitor Resources

```bash
# Pod resource usage
kubectl top pods -n ajasta
kubectl top pods -n postgresql-cluster

# Node resource usage
kubectl top nodes
```

### Database Access

```bash
# Connect to PostgreSQL
kubectl exec -it -n postgresql-cluster cluster-postgresql-1 -- psql -U app -d cluster-postgresql

# Run queries
\conninfo
\dt
SELECT * FROM <table_name>;
```

## 🛠️ Maintenance

### Scaling Deployments

**Scale Backend**:
```bash
kubectl scale deployment/ajasta-backend -n ajasta --replicas=2
```

**Scale Frontend**:
```bash
kubectl scale deployment/ajasta-frontend -n ajasta --replicas=2
```

### Updating Images

```bash
# Update backend image
kubectl set image deployment/ajasta-backend \
  -n ajasta \
  backend=vladimirryrik/ajasta-backend:new-tag

# Update frontend image
kubectl set image deployment/ajasta-frontend \
  -n ajasta \
  frontend=vladimirryrik/ajasta-frontend:new-tag
```

### Database Backups

**Manual backup**:
```bash
# Create backup
kubectl exec -n postgresql-cluster cluster-postgresql-1 -- \
  pg_dump -U app -d cluster-postgresql > backup.sql

# Restore backup
kubectl exec -n postgresql-cluster cluster-postgresql-1 -- \
  psql -U app -d cluster-postgresql < backup.sql
```

### Cleanup

**Remove Ajasta application**:
```bash
# Delete all resources
kubectl delete namespace ajasta

# Delete PostgreSQL (optional - keeps data)
kubectl delete namespace postgresql-cluster
```

## 📖 Additional Resources

### Useful Commands

```bash
# Get all resources in ajasta namespace
kubectl get all -n ajasta

# Get resource usage
kubectl top pods -n ajasta

# Port forward to backend
kubectl port-forward -n ajasta svc/ajasta-backend 8090:8090

# Port forward to frontend
kubectl port-forward -n ajasta svc/ajasta-frontend 3000:80

# Execute command in pod
kubectl exec -it -n ajasta <pod-name> -- sh

# Describe resources
kubectl describe deployment -n ajasta ajasta-backend
kubectl describe service -n ajasta ajasta-backend
kubectl describe ingress -n ajasta ajasta-ingress
```

### Kubernetes Dashboard

If you have Kubernetes Dashboard installed:
```bash
# Get dashboard token
kubectl -n kubernetes-dashboard describe secret \
  $(kubectl -n kubernetes-dashboard get secret | grep admin-user-token | awk '{print $1}')

# Start proxy
kubectl proxy
# Open: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:https/proxy/
```

## 🎯 Best Practices

1. **Always verify deployment** after running playbooks using `25-verify-deployment.yml`
2. **Monitor logs** when first deploying to catch issues early
3. **Test backups regularly** to ensure data recovery works
4. **Use resource limits** to prevent resource exhaustion
5. **Keep images updated** with security patches
6. **Use separate namespaces** for development/staging/production
7. **Enable TLS** for production deployments
8. **Regular backups** of PostgreSQL database
9. **Monitor resource usage** and scale as needed
10. **Document any custom configurations** for your environment

## 📞 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review logs using the provided commands
3. Verify all prerequisites are met
4. Check Kubernetes cluster health

## 📝 Changelog

### Version 1.0.0 (2026-01-14)
- Initial Ansible playbook deployment for Ajasta application
- PostgreSQL HA cluster with CloudNativePG
- Spring Boot Backend API
- React Frontend with Nginx
- NGINX Ingress configuration
- Comprehensive verification playbook

---

**Last Updated**: 2026-01-14
**Maintainer**: Vladimir Rurik
**Kubernetes Version**: 1.34.3
**Application**: Ajasta Appointment Booking Platform

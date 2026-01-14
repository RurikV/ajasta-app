# Quick Start Guide - Ajasta Application Deployment

## 🚀 Fast Deployment (5 minutes)

### Prerequisites Check

```bash
# Verify you have the inventory file
ls -la ../k8s/inventory.ini

# Verify Kubernetes cluster is accessible
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get nodes' --become

# Verify ingress controller is installed
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get svc -n ingress-nginx' --become
```

### Deploy Everything (One Command)

```bash
cd ansible/ajasta-app
./deploy-ajasta.sh
```

**With verbose output (recommended for first deployment):**
```bash
./deploy-ajasta.sh -vv
```

**Resume from a specific step (if deployment fails):**
```bash
./deploy-ajasta.sh 3        # Resume from step 3 (skip steps 1-2)
./deploy-ajasta.sh 3 -vv    # Resume from step 3 with verbose output
```

That's it! The script will:
1. ✅ Deploy PostgreSQL HA cluster
2. ✅ Deploy Backend API
3. ✅ Deploy Frontend
4. ✅ Configure Ingress
5. ✅ Verify everything is working

### Access the Application

```bash
# 1. Get Ingress IP
kubectl get svc -n ingress-nginx ingress-nginx-controller

# 2. Add to /etc/hosts (replace <IP> with actual IP)
echo "<IP> ajasta.local" | sudo tee -a /etc/hosts

# 3. Open in browser
open http://ajasta.local
```

## 📋 What Gets Deployed

| Component | Replicas | Resources | Purpose |
|-----------|----------|-----------|---------|
| PostgreSQL Cluster | 2 (1 primary, 1 replica) | 200m CPU / 256Mi RAM | HA database |
| Backend API | 1 | 500m CPU / 512Mi RAM | Spring Boot API |
| Frontend | 1 | 100m CPU / 64Mi RAM | React/Nginx UI |
| Ingress | - | - | External access |

## 🔍 Verify Deployment

```bash
# Quick verification
./deploy-ajasta.sh --verify-only

# Or check manually
kubectl get pods -n ajasta
kubectl get pods -n postgresql-cluster
kubectl get ingress -n ajasta
```

## 🛠️ Component-Based Deployment

If you prefer to deploy components individually:

```bash
# 1. PostgreSQL only (2 minutes)
ansible-playbook -i ../k8s/inventory.ini 21-deploy-postgres-cluster.yml -vv

# 2. Backend only (1 minute)
ansible-playbook -i ../k8s/inventory.ini 22-deploy-backend.yml -vv

# 3. Frontend only (1 minute)
ansible-playbook -i ../k8s/inventory.ini 23-deploy-frontend.yml -vv

# 4. Ingress only (30 seconds)
ansible-playbook -i ../k8s/inventory.ini 24-deploy-ingress.yml -vv

# 5. Verify (30 seconds)
ansible-playbook -i ../k8s/inventory.ini 25-verify-deployment.yml -vv
```

**Or use the deployment script with step numbers:**
```bash
# Deploy only step 3 (Frontend)
./deploy-ajasta.sh 3 -vv

# Verify only (step 5)
./deploy-ajasta.sh 5 -vv
```

## 🎯 Common Scenarios

### Re-deploy only application (keep database)

```bash
# Start from step 2 (skip PostgreSQL)
./deploy-ajasta.sh 2 -vv
```

### Update frontend only

```bash
# Redeploy step 3 only
./deploy-ajasta.sh 3 -vv
```

### Update backend only

```bash
# Redeploy step 2 only
./deploy-ajasta.sh 2 -vv
```

### Reconfigure ingress only

```bash
# Redeploy step 4 only
./deploy-ajasta.sh 4 -vv
```

### Check if everything is running

```bash
# Run verification only
./deploy-ajasta.sh 5 -vv
```

### Add more replicas

```bash
# Scale backend
kubectl scale deployment/ajasta-backend -n ajasta --replicas=2

# Scale frontend
kubectl scale deployment/ajasta-frontend -n ajasta --replicas=2
```

## 📊 Monitor Deployment

```bash
# Watch pods come up
watch kubectl get pods -n ajasta

# Check backend logs
kubectl logs -n ajasta -l component=backend -f

# Check frontend logs
kubectl logs -n ajasta -l component=frontend -f

# Check database logs
kubectl logs -n postgresql-cluster -l cnpg.io/podRole=instance -f
```

## 🐛 Troubleshooting

### Pods not starting?

```bash
# Check pod status
kubectl get pods -n ajasta

# Describe pod for errors
kubectl describe pod -n ajasta <pod-name>

# Check logs
kubectl logs -n ajasta <pod-name>
```

### Can't access application?

```bash
# Verify ingress
kubectl get ingress -n ajasta

# Check ingress controller
kubectl get svc -n ingress-nginx

# Test from inside cluster
kubectl exec -n ajasta <frontend-pod> -- curl http://localhost
```

### Database connection issues?

```bash
# Check database pods
kubectl get pods -n postgresql-cluster

# Verify database service
kubectl get svc -n postgresql-cluster

# Test connection from backend
kubectl exec -n ajasta <backend-pod> -- nc -zv cluster-postgresql-rw.postgresql-cluster.svc 5432
```

## 📚 Documentation

- **README.md** - Complete documentation
- **DEPLOYMENT_CHECKLIST.md** - Detailed step-by-step guide
- **20-deploy-ajasta-app.yml** - Deployment reference

## 🆘 Need Help?

1. Check the logs: `kubectl logs -n ajasta -l component=<component>`
2. Run verification: `./deploy-ajasta.sh --verify-only`
3. Review README.md for detailed troubleshooting
4. Check DEPLOYMENT_CHECKLIST.md for common issues

## ✅ Success Criteria

You'll know deployment succeeded when:
- ✅ All pods are Running
- ✅ All services have ClusterIP
- ✅ Ingress shows address
- ✅ http://ajasta.local loads in browser
- ✅ API endpoints respond

---

**Time to deploy: ~5 minutes**
**Total size: ~1.2GB (Docker images)**
**Total resources: ~800m CPU / 832Mi RAM**

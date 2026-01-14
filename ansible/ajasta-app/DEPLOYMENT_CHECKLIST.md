# Ajasta Deployment Checklist

This checklist guides you through deploying the Ajasta application to Kubernetes.

## 📋 Pre-Deployment Checklist

### Prerequisites Verification

- [ ] Kubernetes cluster is running (min 2 worker nodes)
- [ ] NGINX Ingress Controller is installed
- [ ] Longhorn storage is installed (or alternative StorageClass)
- [ ] SSH access to master node is configured
- [ ] Ansible is installed (version 2.12+)
- [ ] Docker images are available:
  - [ ] `vladimirryrik/ajasta-backend:alpine` (or custom)
  - [ ] `vladimirryrik/ajasta-frontend:alpine` (or custom)

### Verify Cluster Status

```bash
# Check nodes
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get nodes' --become

# Check ingress controller
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get svc -n ingress-nginx' --become

# Check storage
ansible k8s_master -i ../k8s/inventory.ini -m shell -a 'kubectl get storageclass' --become
```

## 🚀 Deployment Steps

### Option A: Complete Deployment (Recommended)

```bash
cd ansible/ajasta-app
ansible-playbook -i ../k8s/inventory.ini 20-deploy-ajasta-app.yml
```

**Estimated Time**: 10-15 minutes

### Option B: Step-by-Step Deployment

#### Step 1: PostgreSQL Cluster
```bash
ansible-playbook -i ../k8s/inventory.ini 21-deploy-postgres-cluster.yml
```

**Verify**:
```bash
kubectl get cluster -n postgresql-cluster
kubectl get pods -n postgresql-cluster
```

**Expected Output**:
- 2 PostgreSQL pods running (1 primary, 1 replica)
- Cluster status: "Healthy"

---

#### Step 2: Backend API
```bash
ansible-playbook -i ../k8s/inventory.ini 22-deploy-backend.yml
```

**Verify**:
```bash
kubectl get pods -n ajasta -l component=backend
kubectl logs -n ajasta -l component=backend --tail=20
```

**Expected Output**:
- 1 backend pod running
- Logs show "Started AjastaBackendApplication"
- No errors in logs

---

#### Step 3: Frontend
```bash
ansible-playbook -i ../k8s/inventory.ini 23-deploy-frontend.yml
```

**Verify**:
```bash
kubectl get pods -n ajasta -l component=frontend
kubectl logs -n ajasta -l component=frontend --tail=20
```

**Expected Output**:
- 1 frontend pod running
- No errors in logs

---

#### Step 4: Ingress Configuration
```bash
ansible-playbook -i ../k8s/inventory.ini 24-deploy-ingress.yml
```

**Verify**:
```bash
kubectl get ingress -n ajasta
kubectl describe ingress -n ajasta
```

**Expected Output**:
- Ingress resource created
- Backend and frontend routes configured

---

#### Step 5: Verification
```bash
ansible-playbook -i ../k8s/inventory.ini 25-verify-deployment.yml
```

**Expected Output**:
- All components show "✓ READY"
- Overall status: "✓ ALL SYSTEMS OPERATIONAL"

## 🌐 Post-Deployment Configuration

### Configure Local Access

#### 1. Get Ingress Controller IP
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

#### 2. Update /etc/hosts
Add to `/etc/hosts` on your local machine:
```
<INGRESS_IP> ajasta.local
```

#### 3. Test Access
```bash
# Test frontend
curl http://ajasta.local

# Test backend API
curl http://ajasta.local/api

# Open in browser
open http://ajasta.local
```

## ✅ Verification Checklist

### Component Health

- [ ] PostgreSQL cluster is healthy
  ```bash
  kubectl get cluster -n postgresql-cluster
  ```

- [ ] Backend pods are running
  ```bash
  kubectl get pods -n ajasta -l component=backend
  ```

- [ ] Frontend pods are running
  ```bash
  kubectl get pods -n ajasta -l component=frontend
  ```

- [ ] Services are accessible
  ```bash
  kubectl get svc -n ajasta
  ```

- [ ] Ingress is configured
  ```bash
  kubectl get ingress -n ajasta
  ```

### Connectivity Tests

- [ ] Backend → Database
  ```bash
  kubectl exec -n ajasta <backend-pod> -- nc -zv cluster-postgresql-rw.postgresql-cluster.svc 5432
  ```

- [ ] Frontend health endpoint
  ```bash
  kubectl exec -n ajasta <frontend-pod> -- curl http://localhost/health
  ```

- [ ] Ingress access
  ```bash
  curl http://ajasta.local
  curl http://ajasta.local/api
  ```

### Application Tests

- [ ] Frontend loads in browser
- [ ] API endpoints respond correctly
- [ ] User can navigate the application
- [ ] Database connectivity works
- [ ] File uploads work (if configured)

## 🔧 Common Issues and Fixes

### Issue: PostgreSQL pods not ready

**Check**:
```bash
kubectl describe pod -n postgresql-cluster cluster-postgresql-1
kubectl get pvc -n postgresql-cluster
```

**Fix**: Ensure Longhorn is installed and PVCs are bound

### Issue: Backend cannot connect to database

**Check**:
```bash
kubectl logs -n ajasta -l component=backend
kubectl get svc -n postgresql-cluster
```

**Fix**: Verify database service name and namespace are correct

### Issue: Frontend returns 502 errors

**Check**:
```bash
kubectl logs -n ajasta -l component=frontend
kubectl get pods -n ajasta
```

**Fix**: Ensure backend is running and accessible

### Issue: Ingress not accessible

**Check**:
```bash
kubectl get ingress -n ajasta
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

**Fix**:
- Verify /etc/hosts configuration
- Check Ingress Controller is running
- Verify DNS resolution

## 📊 Monitoring Setup

### Enable Logging

```bash
# Follow backend logs
kubectl logs -n ajasta -l component=backend -f

# Follow frontend logs
kubectl logs -n ajasta -l component=frontend -f

# Follow PostgreSQL logs
kubectl logs -n postgresql-cluster cluster-postgresql-1 -f
```

### Resource Monitoring

```bash
# Install metrics-server (if not installed)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Check resource usage
kubectl top pods -n ajasta
kubectl top nodes
```

## 🔐 Security Checklist

### Production Deployment

- [ ] Change default JWT secret
- [ ] Enable TLS for Ingress
- [ ] Configure strong passwords
- [ ] Enable network policies
- [ ] Configure RBAC properly
- [ ] Enable audit logging
- [ ] Regular security updates
- [ ] Backup strategy implemented

### Secrets Management

**Update in playbooks**:
- `jwt_secret` in `22-deploy-backend.yml`
- Database passwords
- API keys (Stripe, AWS, etc.)
- SMTP credentials

**Use Kubernetes Secrets**:
```bash
kubectl create secret generic backend-secrets \
  --from-literal=JWT_SECRET='your-secret-here' \
  --from-literal=STRIPE_SECRET_KEY='sk-live-...' \
  -n ajasta
```

## 📈 Scaling Checklist

### Horizontal Scaling

**Scale Backend**:
```bash
kubectl scale deployment/ajasta-backend -n ajasta --replicas=2
```

**Scale Frontend**:
```bash
kubectl scale deployment/ajasta-frontend -n ajasta --replicas=2
```

### Vertical Scaling

Edit resource limits in playbooks:
- CPU requests/limits
- Memory requests/limits

## 🔄 Updates and Maintenance

### Update Application

1. Build new Docker images
2. Push to registry
3. Update deployment:
```bash
kubectl set image deployment/ajasta-backend \
  -n ajasta \
  backend=vladimirryrik/ajasta-backend:new-tag

kubectl set image deployment/ajasta-frontend \
  -n ajasta \
  frontend=vladimirryrik/ajasta-frontend:new-tag
```

### Database Backups

**Manual Backup**:
```bash
kubectl exec -n postgresql-cluster cluster-postgresql-1 -- \
  pg_dump -U app -d cluster-postgresql > backup-$(date +%Y%m%d).sql
```

**Scheduled Backups**: Configure CloudNativePG backup in `21-deploy-postgres-cluster.yml`

## 🎯 Success Criteria

Deployment is successful when:
- ✅ All pods are running and ready
- ✅ All services are accessible
- ✅ Ingress routes traffic correctly
- ✅ Application loads in browser
- ✅ API endpoints respond
- ✅ Database connectivity works
- ✅ No errors in logs
- ✅ Resource usage is normal

## 📞 Getting Help

If you encounter issues:

1. **Check logs**: `kubectl logs -n ajasta -l component=<component>`
2. **Describe resources**: `kubectl describe <resource> -n ajasta <name>`
3. **Check events**: `kubectl get events -n ajasta --sort-by=.lastTimestamp`
4. **Review README**: See `README.md` for detailed documentation
5. **Verify checklist**: Ensure all items in this checklist are complete

## 📝 Deployment Notes

**Deployment Date**: _____________

**Deployed By**: _____________

**Kubernetes Version**: _____________

**Ingress IP**: _____________

**Issues Encountered**: _____________

**Notes**: _____________

---

**Deployment Status**: [ ] SUCCESS [ ] FAILED

**Next Review Date**: _____________

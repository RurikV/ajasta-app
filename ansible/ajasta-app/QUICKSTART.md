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

## 🔐 TLS/HTTPS Configuration (Optional)

### Enable TLS with Let's Encrypt

The deployment script includes optional TLS setup using Let's Encrypt and cert-manager.

**Requirements:**
1. A public domain name (e.g., `ajasta.yourdomain.com`)
2. DNS A record pointing to your ingress controller
3. Port 80 accessible from the internet
4. Helm installed (`brew install helm` on macOS)

### Deploy with TLS

```bash
# Deploy everything including TLS
./deploy-ajasta.sh 6 -vv

# Or deploy TLS separately after initial deployment
./deploy-ajasta.sh 6 -vv
```

**What happens:**
1. Installs cert-manager (certificate management)
2. Configures Let's Encrypt issuer (starts in staging mode)
3. Updates ingress with TLS configuration
4. Automatically obtains and installs certificates

### Before Running TLS Setup

**IMPORTANT:** You MUST update the configuration in `28-deploy-ingress-tls.yml`:

```yaml
# Change this to your ACTUAL public domain name
tls_host: "ajasta.example.com"  # CHANGE THIS!
```

And update the email in `27-configure-letsencrypt.yml`:

```yaml
# Change to your email for Let's Encrypt notifications
letsencrypt_email: "admin@ajasta.local"  # CHANGE THIS!
```

### DNS Configuration

1. Get your ingress controller external IP or node IP:
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

2. Create an A record in your DNS:
```
ajasta.yourdomain.com  A  <INGRESS_IP_OR_NODE_IP>
```

3. Verify DNS propagation:
```bash
dig ajasta.yourdomain.com
nslookup ajasta.yourdomain.com
```

### Staging vs Production

**Staging (Default):**
- Unlimited certificates
- Fake CA (not trusted by browsers)
- No rate limits
- Perfect for testing

**Production:**
- Real certificates trusted by browsers
- Rate limits apply (50 per week per domain)
- Switch when ready for production use

**To switch to production:**

1. Update `28-deploy-ingress-tls.yml`:
```yaml
letsencrypt_issuer: letsencrypt-prod  # Change from letsencrypt-staging
```

2. Delete existing certificate:
```bash
kubectl delete certificate -n ajasta ajasta-tls
```

3. Re-run TLS deployment:
```bash
./deploy-ajasta.sh 6 -vv
```

### Monitor Certificate Issuance

```bash
# Check certificate status
kubectl get certificate -n ajasta

# View certificate details
kubectl describe certificate -n ajasta ajasta-tls

# Check certificate requests
kubectl get certificaterequest -n ajasta

# View certificate request details
kubectl describe certificaterequest -n ajasta <request-name>

# View cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager -f
```

### Verify TLS is Working

```bash
# Test HTTPS access
curl -I https://ajasta.yourdomain.com/

# Check certificate details
openssl s_client -connect ajasta.yourdomain.com:443 -servername ajasta.yourdomain.com

# View certificate in browser
# Visit https://ajasta.yourdomain.com in your browser
```

### Troubleshooting TLS

**Certificate not issuing?**

1. Check DNS is configured correctly:
```bash
dig ajasta.yourdomain.com
```

2. Verify port 80 is accessible:
```bash
curl http://ajasta.yourdomain.com/
```

3. Check certificate request errors:
```bash
kubectl describe certificaterequest -n ajasta
```

4. Review cert-manager logs:
```bash
kubectl logs -n cert-manager deployment/cert-manager --tail=100
```

**Common errors:**

- `no matching challenge`: DNS not pointing correctly or wrong domain
- `timeout`: Let's Encrypt can't reach your server (check firewall/port 80)
- `rate limit`: Too many certificate requests (use staging for testing)
- `urn:acme:error:unauthorized`: Domain validation failed (check DNS)

**Force certificate retry:**

```bash
# Delete certificate to force re-issuance
kubectl delete certificate -n ajasta ajasta-tls

# Watch new certificate being created
kubectl get certificate -n ajasta -w
```

### Certificate Auto-Renewal

cert-manager automatically renews certificates before they expire (30 days before).

Check renewal status:
```bash
kubectl describe certificate -n ajasta ajasta-tls | grep -A 5 Renewal
```

### Accessing the Application with TLS

Once the certificate is issued and ready:

```bash
# HTTPS access (automatic redirect from HTTP)
Frontend:  https://ajasta.yourdomain.com/
Backend:   https://ajasta.yourdomain.com/api

# HTTP will automatically redirect to HTTPS
http://ajasta.yourdomain.com → https://ajasta.yourdomain.com
```

### Skip TLS Setup

If you don't have a public domain or don't need TLS:

```bash
# Deploy without TLS (HTTP only)
./deploy-ajasta.sh 5

# Access via NodePort
Frontend:  http://<NODE_IP>:32402
Backend:   http://<NODE_IP>:32402/api
```

## 📚 Additional Documentation

- **README.md** - Complete documentation
- **DEPLOYMENT_CHECKLIST.md** - Detailed step-by-step guide
- **20-deploy-ajasta-app.yml** - Deployment reference
- **26-deploy-cert-manager.yml** - cert-manager installation
- **27-configure-letsencrypt.yml** - Let's Encrypt configuration
- **28-deploy-ingress-tls.yml** - TLS ingress setup

# Multi-Environment Deployment Quick Reference

## Quick Commands

### Deploy to Staging
```bash
./scripts/deploy-to-environment.sh staging
```

### Deploy to Production
```bash
./scripts/deploy-to-environment.sh production
```

### Deploy with Verbose Output
```bash
./scripts/deploy-to-environment.sh staging -vv
./scripts/deploy-to-environment.sh production -vvv
```

### Resume from Specific Step
```bash
# Start from step 3 (CloudNativePG deployment)
./scripts/deploy-to-environment.sh staging --step 3

# Start from step 4 (application deployment)
./scripts/deploy-to-environment.sh production --step 4
```

## Deployment Steps

1. **Generate Ansible inventory** from Yandex Cloud VMs
2. **Setup Kubernetes cluster** (k8s components, CNI, storage)
3. **Deploy CloudNativePG** and PostgreSQL cluster
4. **Deploy Ajasta application** (backend, frontend, ingress)
5. **Fix connection timeout** issues

## Environment Differences

| Setting | Staging | Production |
|---------|---------|------------|
| Domain | staging.ajasta.top | ajasta.top |
| Workers | 1 | 3 |
| Backend Replicas | 1 | 2 |
| Frontend Replicas | 1 | 2 |
| PostgreSQL Instances | 1 | 2 (HA) |
| Storage | 5Gi | 20Gi |
| Resources | 256Mi/512Mi | 512Mi/1Gi |

## Configuration Files

### Environment Configuration
- **Staging**: `environments/staging/config.yaml`
- **Production**: `environments/production/config.yaml`

### Helm Values
- **Staging**: `environments/staging/helm-values.yaml`
- **Production**: `environments/production/helm-values.yaml`

### Ansible Variables
- **Staging**: `ansible/k8s/group_vars/environments/staging.yml`
- **Production**: `ansible/k8s/group_vars/environments/production.yml`

### Terraform Variables
- **Staging**: `terraform/staging.tfvars`
- **Production**: `terraform/production.tfvars`

## Verification Commands

### Check Cluster Status
```bash
# SSH to master
ssh -i ~/.ssh/id_rsa_k8s ajasta@<MASTER_IP>

# Check nodes
sudo kubectl get nodes

# Check pods
sudo kubectl get pods -A
sudo kubectl get pods -n ajasta-staging
sudo kubectl get pods -n ajasta
```

### Check Application
```bash
# Get ingress
kubectl get ingress -n ajasta-staging
kubectl get ingress -n ajasta

# Test URLs
curl -I https://staging.ajasta.top
curl -I https://ajasta.top
curl https://ajasta.top/api/actuator/health
```

## Troubleshooting

### Inventory Generation Fails
```bash
# Check VMs are running
yc compute instance list | grep ajasta
```

### Cluster Setup Fails
```bash
# Test SSH connectivity
cd ansible/k8s
ansible -i inventory.ini k8s_master -m ping
```

### Application Not Accessible
```bash
# Check ingress and certificates
kubectl get ingress -n ajasta-staging
kubectl get certificate -n ajasta-staging
kubectl describe certificate -n ajasta-staging
```

## GitLab CI/CD

The pipeline automatically:
1. Plans Terraform (staging & production)
2. Applies Terraform infrastructure
3. Deploys to staging (manual approval)
4. Deploys to production (manual approval)

## Documentation

Full guide: `MULTI_ENVIRONMENT_DEPLOYMENT.md`

## Safety Tips

✓ Always deploy to staging first
✓ Use verbose mode (-vv) for debugging
✓ Verify before deploying to production
✓ Check logs after deployment
✓ Monitor resources regularly

## Support

- Main README: `README.md`
- Terraform docs: `terraform/README.md`
- Multi-Environment: `MULTI_ENVIRONMENT_DEPLOYMENT.md`

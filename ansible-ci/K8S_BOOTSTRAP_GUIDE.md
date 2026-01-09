# Quick Start: Kubernetes Bootstrap

## Prerequisites ✅ (All Done!)

- [x] Terraform resources created (4 VMs in Yandex Cloud)
- [x] Ansible inventory generated (`inventory.ini`)
- [x] Terraform outputs generated (`terraform/outputs.json`)
- [x] SSH connectivity verified to all nodes
- [x] YC credentials available

## Bootstrap Kubernetes

**Step 1: Load credentials (if needed)**
```bash
cd /Users/rurik/IdeaProjects/petrelevich/ajasta-app/ansible-ci
export GITLAB_PAT="glpat-o9enasy8qUsiKZQDBQWP"
source ../scripts/get-gitlab-vars.sh
```

**Step 2: Run bootstrap playbook**
```bash
ansible-playbook -i inventory.ini k8s-bootstrap.yml
```

**Step 3: Verify cluster**
```bash
# SSH to master and check nodes
ssh -i ~/.ssh/id_rsa_k8s ajasta@158.160.79.42
kubectl get nodes
```

## Expected Output

During bootstrap, you'll see:
- ✅ Containerd installation on all nodes
- ✅ Kubernetes package installation
- ✅ Master node initialization
- ✅ Flannel network plugin deployment
- ✅ Worker nodes joining the cluster
- ✅ Cluster ready for applications

## Time Estimate

- **First run**: 15-25 minutes (downloads packages, images)
- **Subsequent runs**: 5-10 minutes (cached packages)

## Cluster Nodes

| Role | IP Address | Hostname |
|------|------------|----------|
| Master | 158.160.79.42 | k8s-master |
| Worker-1 | 158.160.94.84 | k8s-worker-0 |
| Worker-2 | 158.160.71.59 | k8s-worker-1 |
| Worker-3 | 158.160.1.1 | k8s-worker-2 |

## Troubleshooting

**If playbook fails:**
```bash
# Check specific task output
ansible-playbook -i inventory.ini k8s-bootstrap.yml -vvv

# Re-run from specific task
ansible-playbook -i inventory.ini k8s-bootstrap.yml --start-at-task "task name"
```

**Verify connectivity:**
```bash
# Test SSH to master
ssh -i ~/.ssh/id_rsa_k8s ajasta@158.160.79.42 "hostname"

# Test all nodes
ansible k8s -i inventory.ini -m ping
```

**Check bootstrap status:**
```bash
# SSH to master
ssh -i ~/.ssh/id_rsa_k8s ajasta@158.160.79.42

# Check if kubeadm was initialized
sudo kubectl get nodes

# Check pods
sudo kubectl get pods -A
```

## Next Steps After Bootstrap

1. **Install Helm:**
   ```bash
   ansible-playbook -i inventory.ini install-helm.yml
   ```

2. **Deploy applications:**
   ```bash
   ansible-playbook -i inventory.ini deploy-apps.yml
   ```

3. **Check cluster status:**
   ```bash
   ansible-playbook -i inventory.ini status.yml
   ```

## Important Notes

- The playbook uses `kubeadm` for cluster initialization
- Flannel CNI is used for pod networking
- All nodes run containerd as container runtime
- Master node is also schedulable (can run pods)
- SSH key: `~/.ssh/id_rsa_k8s`
- SSH user: `ajasta`

## Cleanup (if needed)

**Destroy cluster:**
```bash
# Keep VMs but remove Kubernetes
ansible-playbook -i inventory.ini destroy-apps.yml

# Or destroy everything via Terraform
cd ../terraform
terraform destroy
```

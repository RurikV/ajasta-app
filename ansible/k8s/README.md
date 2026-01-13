# Kubernetes Cluster Installation with Ansible

This directory contains Ansible playbooks for deploying and managing Kubernetes clusters on k8s-master and k8s-worker nodes.

## 📋 Prerequisites

- Ansible installed on control node
- SSH access to all nodes (k8s-master, k8s-worker-1, k8s-worker-2, k8s-worker-3)
- SSH key: `~/.ssh/id_rsa_k8s`
- All nodes running CentOS Stream 9 or compatible RHEL-based distros

## 📁 Inventory

The inventory file is located at `inventory.ini` with the following structure:

```ini
[cluster_master]
master-node ansible_host=89.169.182.221 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s

[cluster_workers]
worker-node-0 ansible_host=89.169.173.152 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s
worker-node-1 ansible_host=89.169.178.153 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s
worker-node-2 ansible_host=130.193.53.179 ansible_user=ajasta ansible_ssh_private_key_file=~/.ssh/id_rsa_k8s

[k8s_master:children]
cluster_master

[k8s_workers:children]
cluster_workers

[k8s:children]
k8s_master
k8s_workers
```

## 🚀 Quick Start

### Complete Cluster Installation (One Command)

```bash
# Install complete Kubernetes cluster
ansible-playbook -i inventory.ini k8s-install-all.yml
```

This will:
1. Prepare all nodes (firewall, sysctl, kernel modules)
2. Install containerd on all nodes
3. Initialize Kubernetes control plane on master
4. Join worker nodes to the cluster
5. Install Cilium CNI
6. Install Helm package manager

### Step-by-Step Installation

If you prefer to run installation steps individually:

```bash
# 1. Prepare nodes (disable firewall, configure sysctl, load kernel modules)
ansible-playbook -i inventory.ini 01-prepare-nodes.yml

# 2. Install containerd and nerdctl
ansible-playbook -i inventory.ini 5-install-containerd-nerdctl.yml

# 3. Initialize Kubernetes master
ansible-playbook -i inventory.ini 02-init-master.yml

# 4. Join worker nodes
ansible-playbook -i inventory.ini 04-join-workers.yml

# 5. Install Cilium CNI
ansible-playbook -i inventory.ini 03-install-cilium.yml

# 6. Install Helm
ansible-playbook -i inventory.ini 09-install-helm-binary.yml
```

## 💥 Destroy Cluster

To completely remove Kubernetes from all nodes and restore to pre-installation state:

```bash
ansible-playbook -i inventory.ini 00-destroy-k8s-cluster.yml
```

**Warning:** This will:
- Drain and delete all worker nodes from cluster
- Reset kubeadm on all nodes
- Remove Kubernetes packages (kubelet, kubeadm, kubectl)
- Remove containerd and all images
- Delete all configurations and certificates
- Clean up iptables and ipvs rules
- Remove Kubernetes user and directories

## 📦 Additional Components

After base cluster installation, you can install additional components:

### Kubernetes Dashboard
```bash
ansible-playbook -i inventory.ini 10-deploy-kubernetes-dashboard.yml
```

### Ingress NGINX Controller
```bash
ansible-playbook -i inventory.ini 13-deploy-ingress-nginx-controller.yml
```

### Longhorn Storage
```bash
ansible-playbook -i inventory.ini 14-deploy-longhorn-storage.yml
```

### Test Application
```bash
ansible-playbook -i inventory.ini 06-deploy-test-app.yml
```

## 🔍 Verifying Installation

After installation, verify the cluster:

```bash
# SSH to master
ssh ajasta@k8s-master

# Check nodes
sudo kubectl get nodes

# Check all pods
sudo kubectl get pods -A

# Check cluster info
sudo kubectl cluster-info
```

Or from your local machine (after updating kubeconfig):

```bash
# Update kubeconfig
./scripts/update-kubeconfig.sh

# Check cluster
kubectl get nodes
kubectl get pods -A
```

## 📝 Playbook Descriptions

| Playbook | Description |
|----------|-------------|
| `00-destroy-k8s-cluster.yml` | **COMPLETE CLUSTER DESTRUCTION** - Removes all Kubernetes components |
| `01-prepare-nodes.yml` | Prepare nodes (firewall, sysctl, kernel modules) |
| `02-init-master.yml` | Initialize Kubernetes control plane on master |
| `03-install-cilium.yml` | Install Cilium CNI plugin |
| `04-join-workers.yml` | Join worker nodes to cluster |
| `05-expose-hubble.yml` | Expose Cilium Hubble UI |
| `06-deploy-test-app.yml` | Deploy test application (nginx) |
| `09-install-helm-binary.yml` | Install Helm package manager |
| `10-deploy-kubernetes-dashboard.yml` | Deploy Kubernetes Dashboard |
| `13-deploy-ingress-nginx-controller.yml` | Deploy NGINX Ingress Controller |
| `14-deploy-longhorn-storage.yml` | Deploy Longhorn distributed storage |
| `3-disable-firewall-install-consul.yml` | Disable firewall and install Consul |
| `5-install-containerd-nerdctl.yml` | Install containerd and nerdctl |
| `17-deploy-nginx-chart.yml` | Deploy nginx via Helm |
| `19-deploy-cloudnativepg.yml` | Deploy CloudNativePG operator |
| `k8s-install-all.yml` | **MASTER PLAYBOOK** - Run all installation steps |
| `k8s-cluster-setup.sh` | Shell script alternative for cluster setup |

## 🔧 Troubleshooting

### Check if cluster is healthy

```bash
# From master node
sudo kubectl get nodes
sudo kubectl get pods -A
sudo kubectl get cs  # component status (deprecated in newer K8s)
```

### Check specific component

```bash
# Check kubelet
sudo systemctl status kubelet

# Check containerd
sudo systemctl status containerd

# Check kubeadm status
sudo kubeadm version
```

### View logs

```bash
# Kubelet logs
sudo journalctl -u kubelet -f

# Containerd logs
sudo journalctl -u containerd -f

# Cilium pods
sudo kubectl logs -n kube-system -l k8s-app=cilium
```

### Reset specific node

```bash
# On the node
sudo kubeadm reset --force

# Then re-run join playbook
ansible-playbook -i inventory.ini 04-join-workers.yml
```

## 🌐 Accessing Services

### Kubernetes Dashboard
```bash
# Get dashboard token
sudo kubectl -n kubernetes-dashboard describe secret $(sudo kubectl -n kubernetes-dashboard get secret | grep admin-user-token | awk '{print $1}')

# Start proxy
sudo kubectl proxy

# Access at: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:https/proxy/
```

### Hubble UI (Cilium)
```bash
# Port-forward
sudo kubectl port-forward -n kube-system deployment/hubble-ui 12000:12000

# Access at: http://localhost:12000
```

## 🔄 Updating Cluster

To upgrade Kubernetes version:

1. Update the kubernetes_version variable in playbooks
2. Run upgrade on master first, then workers

```bash
# Backup cluster first
./scripts/backup-cluster.sh

# Upgrade master
ansible-playbook -i inventory.ini --limit k8s_master k8s-upgrade.yml -e kubernetes_version=1.29.0

# Upgrade workers
ansible-playbook -i inventory.ini --limit k8s_workers k8s-upgrade.yml -e kubernetes_version=1.29.0
```

## 📊 Current Infrastructure

- **Master**: k8s-master (89.169.168.246)
- **Worker-1**: k8s-worker-1 (89.169.188.79)
- **Worker-2**: k8s-worker-2 (89.169.170.64)
- **Worker-3**: k8s-worker-3 (89.169.170.237)

## 🔐 Security Notes

- All nodes use SSH key authentication (`~/.ssh/id_rsa_k8s`)
- Firewall disabled on all nodes (managed by cloud provider)
- SELinux set to permissive mode
- Containerd runs with SystemdCgroup enabled

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Cilium Documentation](https://docs.cilium.io/)
- [Helm Documentation](https://helm.sh/docs/)
- [Ansible Documentation](https://docs.ansible.com/)

## ⚡ Quick Reference

```bash
# Install complete cluster
ansible-playbook -i inventory.ini k8s-install-all.yml

# Check cluster status
ansible k8s_master -i inventory.ini -m shell -a 'kubectl get nodes' --become

# Destroy cluster
ansible-playbook -i inventory.ini 00-destroy-k8s-cluster.yml

# Deploy apps
ansible-playbook -i inventory.ini 06-deploy-test-app.yml
```

---

**Last Updated**: 2025-01-13
**Kubernetes Version**: 1.34.3
**CNI**: Cilium
**Maintainer**: Vladimir Rurik

# k8s_cluster Ansible Role

[![Galaxy](https://img.shields.io/badge/galaxy-ajasta.k8s_cluster-blue.svg)](https://galaxy.ansible.com/ajasta/k8s_cluster)

An Ansible role to bootstrap Kubernetes clusters using kubeadm.

## Requirements

- Ansible >= 2.9
- Python >= 3.6
- System Requirements:
  - Ubuntu 20.04/22.04 or CentOS/RHEL 8/9
  - Root or sudo access
  - 2GB+ RAM per node
  - 2+ CPUs per node
  - Network connectivity between nodes

## Dependencies

This role depends on:
- `k8s_prerequisites` - Prepares nodes for Kubernetes
- `containerd` - Installs container runtime

## Role Variables

### Cluster Configuration

```yaml
# Kubernetes version
kubernetes_version: "1.29.0"
kubernetes_version_prefix: "1.29"

# Network configuration
kubernetes_pod_network_cidr: "10.244.0.0/16"
kubernetes_service_subnet_cidr: "10.96.0.0/12"
kubernetes_dns_domain: "cluster.local"

# Control plane
kubernetes_apiserver_advertise_address: "{{ ansible_default_ipv4.address }}"
kubernetes_control_plane_endpoint: ""  # For HA clusters
```

### Node Configuration

```yaml
# Node roles
kubernetes_node_role: "worker"  # Options: control-plane, worker
kubernetes_init_control_plane: false  # Set to true for first control-plane node
kubernetes_join_cluster: false  # Set to true for worker nodes

# Container runtime
kubernetes_container_runtime: "containerd"
kubernetes_cri_socket: "/run/containerd/containerd.sock"
```

## Example Playbook

### Initialize Control Plane

```yaml
---
- hosts: k8s_master
  become: yes
  roles:
    - role: k8s_prerequisites
    - role: containerd
    - role: k8s_cluster
      vars:
        kubernetes_init_control_plane: true
        kubernetes_pod_network_cidr: "10.244.0.0/16"
```

### Join Worker Nodes

```yaml
---
- hosts: k8s_workers
  become: yes
  roles:
    - role: k8s_prerequisites
    - role: containerd
    - role: k8s_cluster
      vars:
        kubernetes_join_cluster: true
```

## Installation

### From Ansible Galaxy

```bash
ansible-galaxy role install ajasta.k8s_cluster
```

### From Git Repository

```bash
git clone https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster.git
cd k8s-cluster/roles/k8s_cluster
ansible-galaxy role install -r requirements.yml
```

## Usage

### Step 1: Prepare Control Plane Node

```yaml
- hosts: k8s_control_plane
  become: yes
  tasks:
    - name: Initialize Kubernetes cluster
      include_role:
        name: k8s_cluster
      vars:
        kubernetes_init_control_plane: true
```

### Step 2: Join Worker Nodes

After the control plane is initialized, it will generate a join command at `/tmp/kubernetes-join-command.sh`. Copy this file to worker nodes and execute:

```bash
# On control plane
cat /tmp/kubernetes-join-command.sh

# On worker nodes
ansible k8s_workers -m copy -a "src=/tmp/kubernetes-join-command.sh dest=/tmp/join.sh mode=0755"
ansible k8s_workers -m shell -a "/tmp/join.sh"
```

### Or use Ansible:

```yaml
- hosts: k8s_workers
  become: yes
  tasks:
    - name: Join worker nodes to cluster
      include_role:
        name: k8s_cluster
      vars:
        kubernetes_join_cluster: true
```

## What Gets Installed

1. **kubeadm** - Kubernetes cluster bootstrapping tool
2. **kubelet** - Kubernetes node agent
3. **kubectl** - Kubernetes command-line tool
4. **CNI plugins** - Container Network Interface plugins
5. **Systemd services** - kubelet and containerd services
6. **Kubeconfig** - Admin access configuration

## Post-Installation

### Verify Cluster Status

```bash
# On control plane
kubectl get nodes
kubectl cluster-info
kubectl get componentstatuses
```

### Install Network Plugin

```bash
# Install Cilium CNI
kubectl apply -f https://raw.githubusercontent.com/cilium/cilium/v1.14/install/kubernetes/quick-install.yaml

# Or install Calico
kubectl apply -f https://docs.projectcalico.org/manifests/calico.yaml
```

### Remove Master Taint (Optional)

For single-node clusters or to run pods on control plane:

```bash
kubectl taint nodes --all node-role.kubernetes.io/control-plane-
```

## Cluster Access

### From Control Plane

```bash
# Root user has access automatically
kubectl get nodes

# Other users
mkdir -p ~/.kube
sudo cp -i /etc/kubernetes/admin.conf ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
```

### From Remote Machine

```bash
# Copy kubeconfig from control plane
scp user@control-plane:/etc/kubernetes/admin.conf ~/.kube/config

# Update server endpoint
sed -i 's/server: .*/server: https:\/\/<control-plane-ip>:6443/' ~/.kube/config
```

## Troubleshooting

### Check Service Status

```bash
sudo systemctl status kubelet
sudo systemctl status containerd
```

### View Logs

```bash
# Kubelet logs
sudo journalctl -u kubelet -f

# Container logs
sudo journalctl -u containerd -f

# Kubernetes pods
kubectl logs -n kube-system <pod-name>
```

### Reset Node

To remove Kubernetes configuration from a node:

```bash
sudo kubeadm reset
sudo rm -rf /etc/cni/net.d
sudo iptables -F && sudo iptables -t nat -F && sudo iptables -t mangle -F && sudo iptables -X
```

### Common Issues

**Issue**: kubelet fails to start
```bash
# Check swap is disabled
sudo swapon --show

# Check CNI plugins are installed
ls /opt/cni/bin/

# Check containerd is running
sudo ctr version
```

**Issue**: Nodes remain NotReady
```bash
# Install network plugin
kubectl apply -f <network-plugin-yaml>

# Check kubelet logs
sudo journalctl -u kubelet -f
```

**Issue**: Join token expired
```bash
# Generate new token on control plane
sudo kubeadm token create --print-join-command

# Or just generate new token
sudo kubeadm token create
```

## High Availability

For HA control plane, set up a load balancer and configure:

```yaml
kubernetes_control_plane_endpoint: "LOAD_BALANCER_DNS:LOAD_BALANCER_PORT"
kubernetes_control_plane_replicas: 3
```

Then initialize multiple control plane nodes with the same endpoint.

## Upgrading Kubernetes

```bash
# Check available versions
apt-cache madison kubeadm

# Upgrade control plane
sudo apt-get update
sudo apt-get install -y kubeadm=<version>
sudo kubeadm upgrade plan
sudo kubeadm upgrade apply <version>

# Upgrade kubelet and kubectl
sudo apt-get install -y kubelet=<version> kubectl=<version>
sudo systemctl daemon-reload
sudo systemctl restart kubelet
```

## License

MIT

## Author Information

- **Author**: Ajasta DevOps Team
- **Email**: devops@ajasta.top

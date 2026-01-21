# Ajasta Kubernetes Cluster Collection

[![Galaxy](https://img.shields.io/badge/ansible_galaxy-ajasta.k8s_cluster-blue.svg)](https://galaxy.ansible.com/ajasta/k8s_cluster)

A comprehensive Ansible collection for deploying and managing Kubernetes clusters using kubeadm and containerd.

## Description

This collection provides roles, playbooks, and modules for:

- Preparing Linux nodes for Kubernetes
- Installing and configuring containerd as container runtime
- Bootstrapping Kubernetes clusters with kubeadm
- Managing cluster configuration and networking
- Deploying cluster add-ons (CNI, ingress, storage)

## Installation

### From Ansible Galaxy

```bash
ansible-galaxy collection install ajasta.k8s_cluster
```

### From Git Repository

```bash
git clone https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster.git
cd k8s-cluster
ansible-galaxy collection build --force
ansible-galaxy collection install ajasta-k8s_cluster-1.0.0.tar.gz
```

## Roles Included

### k8s_prerequisites

Prepare Linux nodes for Kubernetes cluster deployment.

**Features:**
- Disable swap
- Configure kernel modules (overlay, br_netfilter)
- Apply sysctl settings for Kubernetes networking
- Install required packages
- Configure firewall

```yaml
- hosts: k8s_nodes
  become: yes
  roles:
    - ajasta.k8s_cluster.k8s_prerequisites
```

### containerd

Install and configure containerd as the container runtime.

**Features:**
- Install containerd binary
- Download and install CNI plugins
- Configure systemd cgroup driver
- Optional nerdctl CLI installation
- Generate optimized configuration

```yaml
- hosts: k8s_nodes
  become: yes
  roles:
    - ajasta.k8s_cluster.containerd
```

### k8s_cluster

Bootstrap Kubernetes cluster using kubeadm.

**Features:**
- Install kubeadm, kubelet, kubectl
- Initialize control plane
- Generate join tokens
- Join worker nodes
- Configure kubectl access

```yaml
# Initialize control plane
- hosts: k8s_master
  become: yes
  roles:
    - ajasta.k8s_cluster.k8s_cluster
  vars:
    kubernetes_init_control_plane: true

# Join worker nodes
- hosts: k8s_workers
  become: yes
  roles:
    - ajasta.k8s_cluster.k8s_cluster
  vars:
    kubernetes_join_cluster: true
```

## Usage Example

### Complete Cluster Deployment

```yaml
---
- name: Deploy Kubernetes cluster
  hosts: k8s_nodes
  become: yes
  tasks:
    - name: Prepare nodes
      include_role:
        name: ajasta.k8s_cluster.k8s_prerequisites

    - name: Install containerd
      include_role:
        name: ajasta.k8s_cluster.containerd

- name: Initialize control plane
  hosts: k8s_control_plane
  become: yes
  tasks:
    - name: Bootstrap cluster
      include_role:
        name: ajasta.k8s_cluster.k8s_cluster
      vars:
        kubernetes_init_control_plane: true
        kubernetes_pod_network_cidr: "10.244.0.0/16"

- name: Join worker nodes
  hosts: k8s_workers
  become: yes
  tasks:
    - name: Add workers to cluster
      include_role:
        name: ajasta.k8s_cluster.k8s_cluster
      vars:
        kubernetes_join_cluster: true
```

## Requirements

- Ansible >= 2.9
- Python >= 3.6
- Target systems:
  - Ubuntu 20.04/22.04
  - CentOS/RHEL 8/9
- Minimum hardware per node:
  - 2GB RAM
  - 2 CPUs
  - 10GB disk

## Dependencies

- `community.general` >= 6.0.0
- `ansible.posix` >= 1.5.0

## Configuration

All roles support extensive configuration via variables. See individual role documentation for details.

### Common Variables

```yaml
# Kubernetes version
kubernetes_version: "1.29.0"

# Pod network CIDR
kubernetes_pod_network_cidr: "10.244.0.0/16"

# Service network CIDR
kubernetes_service_subnet_cidr: "10.96.0.0/12"

# Container runtime
kubernetes_container_runtime: "containerd"
kubernetes_cri_socket: "/run/containerd/containerd.sock"
```

## Post-Installation

After cluster deployment, you'll need to:

1. Install a CNI plugin (Cilium, Calico, Flannel)
2. Install ingress controller (NGINX, Traefik)
3. Configure storage (Longhorn, Ceph)
4. Deploy monitoring (Prometheus, Grafana)

Example:

```bash
# Install Cilium CNI
kubectl apply -f https://raw.githubusercontent.com/cilium/cilium/v1.14/install/kubernetes/quick-install.yaml
```

## Testing

Each role includes Molecule tests:

```bash
# Install testing tools
pip install ansible-molecule molecule-docker ansible-lint yamllint

# Run tests
cd collections/ajasta/k8s_cluster/roles/k8s_prerequisites
molecule test
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT

## Author Information

- **Author**: Ajasta DevOps Team
- **Email**: devops@ajasta.top
- **GitHub**: https://github.com/RurikV/ajasta-ansible-automation

## Support

For issues, questions, or contributions:
- GitHub Issues: https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster/issues
- Documentation: https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster/docs
- Email: devops@ajasta.top

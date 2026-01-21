# Ansible Role-Based Architecture Design

## Overview

Refactor monolithic Ansible playbooks into a modular, testable, and publishable structure using:
- **Ansible Roles** - Reusable, single-purpose components
- **Molecule** - Automated testing framework
- **Ansible Galaxy** - Public publishing platform
- **Multiple Repositories** - Separation of concerns

## Repository Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                    GitLab Organization                       │
│                   ajasta-ansible-automation                  │
└─────────────────────────────────────────────────────────────────┘
                            │
          ┌─────────────────┴──────────────────┐
          │                                     │
    ┌─────▼─────────┐              ┌─────────▼────────┐
    │ k8s-cluster   │              │  ajasta-app     │
    │ Ansible Role  │              │  Ansible Role   │
    │ Repository    │              │  Repository     │
    └───────────────┘              └──────────────────┘
```

## Repository 1: ansible-k8s-cluster

**Purpose:** Kubernetes cluster infrastructure roles

**URL:** `https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster`

### Roles Included:

1. **k8s_prerequisites** - Prepare nodes for Kubernetes
   - Disable swap
   - Configure sysctl
   - Load kernel modules
   - Configure firewall

2. **containerd** - Install container runtime
   - Install containerd
   - Install nerdctl
   - Configure CNI plugins

3. **k8s_cluster** - Bootstrap Kubernetes cluster
   - Initialize control plane
   - Join worker nodes
   - Configure kubelet
   - Setup kubectl

4. **cilium_cni** - Install Cilium CNI
   - Deploy Cilium
   - Configure network policies
   - Setup Hubble observability

5. **helm** - Install Helm package manager
   - Install Helm binary
   - Configure repositories
   - Setup plugins

6. **ingress_nginx** - Deploy NGINX Ingress Controller
   - Install ingress controller
   - Configure NodePorts
   - Setup TCP/UDP services

7. **longhorn** - Deploy Longhorn distributed storage
   - Install Longhorn
   - Configure storage classes
   - Setup UI dashboard

8. **cloudnativepg** - Deploy CloudNativePG operator
   - Install operator
   - Configure PostgreSQL clusters
   - Setup backups

9. **cert_manager** - Deploy cert-manager
   - Install cert-manager
   - Configure ClusterIssuers
   - Setup Let's Encrypt

10. **metrics_server** - Kubernetes Metrics Server
    - Install metrics-server
    - Configure resource metrics

11. **kubernetes_dashboard** - Kubernetes Dashboard
    - Deploy dashboard
    - Configure admin user
    - Setup ingress

### Directory Structure:

```
ansible-k8s-cluster/
├── roles/
│   ├── k8s_prerequisites/
│   │   ├── tasks/
│   │   │   └── main.yml
│   │   ├── handlers/
│   │   │   └── main.yml
│   │   ├── defaults/
│   │   │   └── main.yml
│   │   ├── vars/
│   │   │   ├── main.yml
│   │   │   ├── Debian.yml
│   │   │   └── RedHat.yml
│   │   ├── meta/
│   │   │   └── main.yml
│   │   ├── molecule/
│   │   │   ├── default/
│   │   │   │   ├── converge.yml
│   │   │   │   └── molecule.yml
│   │   │   └── centos/
│   │   │       ├── converge.yml
│   │   │       └── molecule.yml
│   │   ├── tests/
│   │   │   └── test.yml
│   │   ├── README.md
│   │   └── galaxy.yml
│   ├── containerd/
│   │   └── ...
│   ├── k8s_cluster/
│   │   └── ...
│   └── ... (other roles)
├── collections/
│   └── ajasta/
│       └── k8s_cluster/
│           ├── playbooks/
│           │   ├── setup_cluster.yml
│           │   └── upgrade_cluster.yml
│           ├── plugins/
│           │   └── modules/
│           └── galaxy.yml
├── playbooks/
│   ├── create_cluster.yml
│   └── destroy_cluster.yml
├── inventory/
│   ├── group_vars/
│   │   ├── all.yml
│   │   └── k8s_nodes.yml
│   └── hosts.yml
├── requirements.yml
├── requirements-dev.yml
├── ansible.cfg
└── README.md
```

### Molecule Testing:

Each role includes Molecule tests:

```yaml
# roles/k8s_prerequisites/molecule/default/molecule.yml
dependency:
  name: galaxy
driver:
  name: delegated
platforms:
  - name: instance
    image: ubuntu:22.04
provisioner:
  name: ansible
verifier:
  name: ansible
```

### Example Role: k8s_prerequisites

**meta/main.yml:**
```yaml
galaxy_info:
  author: Ajasta Team
  description: Prepare Linux nodes for Kubernetes cluster
  company: Ajasta
  license: MIT
  min_ansible_version: "2.9"
  role_name: k8s_prerequisites
  galaxy_tags:
    - kubernetes
    - k8s
    - cluster
    - prerequisites
    - system
```

**tasks/main.yml:**
```yaml
---
- name: Disable swap
  ansible.posix.sysctl:
    name: vm.swappiness
    value: 1
    state: present

- name: Load required kernel modules
  community.general.modprobe:
    name: "{{ item }}"
    state: present
  loop:
    - overlay
    - br_netfilter
    - ip_tables
```

## Repository 2: ansible-ajasta-app

**Purpose:** Ajasta application deployment roles

**URL:** `https://github.com/RurikV/ajasta-ansible-automation/ajasta-app`

### Roles Included:

1. **postgresql_cluster** - CloudNativePG PostgreSQL
   - Deploy operator
   - Create cluster
   - Configure replication
   - Setup backups

2. **ajasta_backend** - Spring Boot backend
   - Deploy backend
   - Configure environment
   - Setup secrets
   - Configure resources

3. **ajasta_frontend** - React frontend
   - Deploy frontend
   - Configure ingress
   - Setup resources

4. **ajasta_ingress** - Application ingress
   - Create ingress resources
   - Configure TLS
   - Setup routing

5. **ajasta_full_stack** - Complete application
   - PostgreSQL + Backend + Frontend + Ingress
   - End-to-end deployment
   - Health checks

6. **connection_timeout_fix** - Fix ERR_CONNECTION_TIMED_OUT
   - Attach static IP
   - Setup socat forwarding
   - Configure iptables

### Directory Structure:

```
ansible-ajasta-app/
├── roles/
│   ├── postgresql_cluster/
│   │   ├── tasks/
│   │   │   ├── deploy_operator.yml
│   │   │   ├── create_cluster.yml
│   │   │   └── main.yml
│   │   ├── templates/
│   │   │   ├── cluster.yml
│   │   │   └── backup_schedule.yml
│   │   ├── defaults/
│   │   │   └── main.yml
│   │   ├── vars/
│   │   │   ├── main.yml
│   │   │   ├── staging.yml
│   │   │   └── production.yml
│   │   ├── molecule/
│   │   │   ├── default/
│   │   │   │   └── molecule.yml
│   │   │   └── kind/
│   │   │       ├── molecule.yml
│   │   │       └── kind.yml
│   │   └── galaxy.yml
│   ├── ajasta_backend/
│   │   └── ...
│   ├── ajasta_frontend/
│   │   └── ...
│   └── ajasta_ingress/
│       └── ...
├── collections/
│   └── ajasta/
│       └── app/
│           ├── playbooks/
│           │   ├── deploy_full.yml
│           │   ├── deploy_backend.yml
│           │   └── deploy_frontend.yml
│           └── galaxy.yml
├── playbooks/
│   ├── deploy_full_stack.yml
│   └── deploy_database.yml
├── inventory/
│   └── group_vars/
│       ├── staging.yml
│       └── production.yml
└── README.md
```

### Environment-Specific Configuration:

**inventory/group_vars/staging.yml:**
```yaml
application:
  namespace: ajasta-staging
  replicas: 1

postgresql:
  instances: 1
  storage_size: 5Gi

ingress:
  host: staging.ajasta.top
```

**inventory/group_vars/production.yml:**
```yaml
application:
  namespace: ajasta
  replicas: 2

postgresql:
  instances: 2
  storage_size: 20Gi

ingress:
  host: ajasta.top
```

## Molecule Testing Strategy

### Unit Testing with Docker:

```yaml
# roles/ajasta_backend/molecule/default/molecule.yml
---
dependency:
  name: galaxy
driver:
  name: docker
platforms:
  - name: ubuntu22
    image: ubuntu:22.04
    pre_build_image: true
    command: sleep infinity
    privileged: true
provisioner:
  name: ansible
  playbooks:
    converge: converge.yml
    verify: verify.yml
verifier:
  name: ansible
lint: |
  set -e
  yamllint .
  ansible-lint .
```

### Integration Testing with Kind:

```yaml
# roles/postgresql_cluster/molecule/kind/molecule.yml
---
dependency:
  name: galaxy
driver:
  name: podman
platforms:
  - name: kind
    groups:
      - k8s
provisioner:
  name: ansible
  playbooks:
    converge: converge.yml
    verify: verify.yml
verifier:
  name: ansible
```

## Ansible Galaxy Publishing

### Galaxy.yml for k8s-cluster collection:

```yaml
# collections/ajasta/k8s_cluster/galaxy.yml
namespace: ajasta
name: k8s_cluster
version: 1.0.0
authors:
  - Ajasta Team
description: Kubernetes cluster deployment and management collection
license: MIT
license_file: LICENSE
tags:
  - kubernetes
  - k8s
  - cluster
  - cloud
  - infrastructure
  - deployment
  - automation
dependencies:
  community.general: ">=6.0.0"
  community.docker: ">=3.0.0"
  kubernetes.core: ">=2.0.0"
```

### Galaxy.yml for ajasta-app collection:

```yaml
# collections/ajasta/app/galaxy.yml
namespace: ajasta
name: app
version: 1.0.0
authors:
  - Ajasta Team
description: Ajasta application deployment collection
license: MIT
tags:
  - ajasta
  - spring-boot
  - react
  - postgresql
  - application
  - fullstack
dependencies:
  ajasta.k8s_cluster: ">=1.0.0"
  cloudnativepg.cloudnativepg: ">=0.0.1"
```

## Usage Examples

### Using k8s-cluster collection:

```yaml
---
# create_cluster.yml
- name: Deploy Kubernetes cluster
  hosts: k8s_master
  become: yes
  roles:
    - role: ajasta.k8s_cluster.k8s_prerequisites
    - role: ajasta.k8s_cluster.containerd
    - role: ajasta.k8s_cluster.k8s_cluster
    - role: ajasta.k8s_cluster.cilium_cni
    - role: ajasta.k8s_cluster.ingress_nginx
```

### Using ajasta-app collection:

```yaml
---
# deploy_app.yml
- name: Deploy Ajasta application
  hosts: k8s_master
  become: yes
  roles:
    - role: ajasta.app.postgresql_cluster
    - role: ajasta.app.ajasta_backend
    - role: ajasta.app.ajasta_frontend
    - role: ajasta.app.ajasta_ingress
```

## CI/CD Integration

### GitLab CI/CD for k8s-cluster:

```yaml
# .gitlab-ci.yml
stages:
  - lint
  - molecule
  - publish

molecule:test:
  stage: molecule
  image: quay.io/ansible/molecule-test:latest
  script:
    - for role in roles/*; do cd $role && molecule test || true; done
  tags:
    - docker

galaxy:publish:
  stage: publish
  image: python:3.11
  script:
    - ansible-galaxy collection build --force
    - ansible-galaxy collection publish
  only:
    - main
```

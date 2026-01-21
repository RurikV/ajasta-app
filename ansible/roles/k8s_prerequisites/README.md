# k8s_prerequisites Ansible Role

[![Galaxy](https://img.shields.io/badge/galaxy-ajasta.k8s_prerequisites-blue.svg)](https://galaxy.ansible.com/ajasta/k8s_prerequisites)

An Ansible role to prepare Linux nodes for Kubernetes cluster deployment.

## Requirements

- Ansible >= 2.9
- Python >= 3.6
- System Requirements:
  - Ubuntu 20.04/22.04 or CentOS/RHEL 8/9
  - Root or sudo access

## Role Variables

### Default Variables

```yaml
# Disable swap
k8s_disable_swap: true

# Sysctl settings
k8s_sysctl_settings:
  net.bridge.bridge-nf-call-iptables: 1
  net.ipv4.ip_forward: 1

# Kernel modules to load
k8s_kernel_modules:
  - overlay
  - br_netfilter

# Packages to install
k8s_required_packages:
  - apt-transport-https
  - ca-certificates
```

## Dependencies

None.

## Example Playbook

```yaml
---
- hosts: k8s_nodes
  become: yes
  roles:
    - role: k8s_prerequisites
```

## Installation

### From Ansible Galaxy

```bash
ansible-galaxy role install ajasta.k8s_prerequisites
```

### From Git Repository

```bash
git clone https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster.git
cd k8s-cluster/roles/k8s_prerequisites
ansible-galaxy role install -r requirements.yml
```

## Molecule Testing

This role includes Molecule tests for automated validation:

### Prerequisites

```bash
# Install Molecule and Docker driver
pip install ansible-molecule molecule-docker ansible-lint yamllint

# Or on Ubuntu/Debian
apt-get install python3-pip
pip3 install ansible-molecule molecule-docker ansible-lint yamllint
```

### Running Tests

```bash
# Run complete molecule test (lint, create, converge, verify, destroy)
molecule test

# Run only converge (create and apply role)
molecule converge

# Run only verification
molecule verify

# Run on specific platform
molecule test --platform-name ubuntu22
molecule test --platform-name centos8

# Destroy test instances
molecule destroy

# Run with verbose output
molecule test --debug
```

### Testing Scenarios

The role includes two test scenarios:

1. **Default Scenario** (`molecule/default/`)
   - Tests on Ubuntu 22.04
   - Tests on CentOS 8
   - Verifies kernel modules, sysctl settings, swap, packages

2. **Custom Scenarios**
   - Can be added for specific Kubernetes versions
   - Can test different OS distributions

## Testing

The role includes tests that verify:
- Swap is disabled
- Kernel modules are loaded (overlay, br_netfilter, etc.)
- Sysctl settings are applied
- Required packages are installed
- Firewall is configured correctly
- `/etc/modules-load.d/k8s.conf` exists with correct content

## CI/CD Integration

This role is designed to work with CI/CD pipelines:

```yaml
# .gitlab-ci.yml example
molecule:test:
  stage: test
  image: quay.io/ansible/molecule-test:latest
  script:
    - molecule test
  tags:
    - docker

ansible:lint:
  stage: lint
  image: python:3.11
  script:
    - pip install ansible-lint yamllint
    - ansible-lint
    - yamllint .
```

## License

MIT

## Author Information

- **Author**: Ajasta DevOps Team
- **Email**: devops@ajasta.top

# containerd Ansible Role

[![Galaxy](https://img.shields.io/badge/galaxy-ajasta.containerd-blue.svg)](https://galaxy.ansible.com/ajasta/containerd)

An Ansible role to install and configure containerd as the container runtime for Kubernetes clusters.

## Requirements

- Ansible >= 2.9
- Python >= 3.6
- System Requirements:
  - Ubuntu 20.04/22.04 or CentOS/RHEL 8/9
  - Root or sudo access
  - Internet connectivity for downloading packages

## Role Variables

### Default Variables

```yaml
# containerd version
containerd_version: "1.7.13"

# CNI plugins version
cni_plugins_version: "1.4.0"

# Installation directories
containerd_binary_dir: "/usr/local/bin"
containerd_config_dir: "/etc/containerd"
cni_bin_dir: "/opt/cni/bin"

# Cgroup driver
containerd_cgroup_driver: "systemd"

# Install nerdctl (optional CLI tool)
nerdctl_install: true

# Enable and start containerd service
containerd_service_enabled: true
containerd_service_state: started
```

### Advanced Configuration

```yaml
# Snapshotter
containerd_snapshotter: "overlayfs"

# Use systemd cgroup driver (required for Kubernetes)
containerd_use_systemd_cgroup: true

# Registry mirrors (optional)
containerd_registry_mirrors:
  - "https://registry-1.docker.io"

# Log level
containerd_log_level: "info"

# Metrics
containerd_metrics_address: ""
```

## Dependencies

None.

## Example Playbook

### Basic Usage

```yaml
---
- hosts: k8s_nodes
  become: yes
  roles:
    - role: containerd
```

### With Custom Configuration

```yaml
---
- hosts: k8s_nodes
  become: yes
  roles:
    - role: containerd
      vars:
        containerd_version: "1.7.13"
        cni_plugins_version: "1.4.0"
        containerd_use_systemd_cgroup: true
        nerdctl_install: false
```

## Installation

### From Ansible Galaxy

```bash
ansible-galaxy role install ajasta.containerd
```

### From Git Repository

```bash
git clone https://github.com/RurikV/ajasta-ansible-automation/k8s-cluster.git
cd k8s-cluster/roles/containerd
ansible-galaxy role install -r requirements.yml
```

## Molecule Testing

### Prerequisites

```bash
# Install Molecule and Docker driver
pip install ansible-molecule molecule-docker ansible-lint yamllint
```

### Running Tests

```bash
# Run complete molecule test
molecule test

# Run only converge
molecule converge

# Run only verification
molecule verify

# Destroy test instances
molecule destroy
```

## Testing

The role includes tests that verify:
- containerd binary is installed
- CNI plugins are installed in /opt/cni/bin
- containerd service is active and running
- Configuration file exists in /etc/containerd/config.toml
- Systemd cgroup driver is configured
- ctr command works correctly

## What Gets Installed

1. **containerd** - Container runtime daemon
   - Binary: `/usr/local/bin/containerd`
   - Service: `containerd.service`

2. **CNI plugins** - Container Network Interface plugins
   - Location: `/opt/cni/bin/`
   - Includes: bridge, host-local, loopback, etc.

3. **nerdctl** (optional) - Docker-compatible CLI for containerd
   - Binary: `/usr/local/bin/nerdctl`
   - Can be disabled with `nerdctl_install: false`

4. **containerdctl** - Control tool for containerd
   - Binary: `/usr/local/bin/ctr`

## Kubernetes Integration

This role is designed specifically for Kubernetes clusters:

- Configures systemd cgroup driver (required by Kubernetes)
- Downloads and installs CNI plugins for networking
- Sets up proper systemd service configuration
- Generates default configuration optimized for Kubernetes

## Troubleshooting

### Check containerd status

```bash
sudo systemctl status containerd
sudo systemctl is-active containerd
```

### View containerd logs

```bash
sudo journalctl -u containerd -f
```

### Test containerd

```bash
# Check version
sudo ctr version

# List containers
sudo ctr containers list

# List images
sudo ctr images list

# Pull an image
sudo ctr images pull docker.io/library/alpine:latest
```

### Restart containerd

```bash
sudo systemctl restart containerd
```

## CI/CD Integration

```yaml
# .gitlab-ci.yml example
molecule:test:
  stage: test
  image: quay.io/ansible/molecule-test:latest
  script:
    - molecule test
  tags:
    - docker
```

## License

MIT

## Author Information

- **Author**: Ajasta DevOps Team
- **Email**: devops@ajasta.top

#!/usr/bin/env bash
# Generate ansible inventory from Yandex Cloud VMs
# This script fetches the IPs of k8s-master, k8s-worker-1, and k8s-worker-2
# and creates a proper inventory.ini file

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INVENTORY_FILE="${SCRIPT_DIR}/inventory.ini"

echo "Fetching VM information from Yandex Cloud..."

# Get master public IP (one_to_one_nat address)
MASTER_IP=$(yc compute instance get k8s-master --format json 2>/dev/null | \
  jq -r '.network_interfaces[0].primary_v4_address.one_to_one_nat.address // empty' || echo "")

if [ -z "$MASTER_IP" ]; then
  echo "ERROR: Could not find public IP for k8s-master" >&2
  echo "Make sure the VM exists and has a public IP assigned" >&2
  exit 1
fi

echo "Master public IP: $MASTER_IP"

# Get worker internal IPs
WORKER1_IP=$(yc compute instance get k8s-worker-1 --format json 2>/dev/null | \
  jq -r '.network_interfaces[0].primary_v4_address.address // empty' || echo "")

WORKER2_IP=$(yc compute instance get k8s-worker-2 --format json 2>/dev/null | \
  jq -r '.network_interfaces[0].primary_v4_address.address // empty' || echo "")

if [ -z "$WORKER1_IP" ] || [ -z "$WORKER2_IP" ]; then
  echo "WARNING: Could not find all worker IPs" >&2
  echo "Worker1 IP: ${WORKER1_IP:-NOT FOUND}" >&2
  echo "Worker2 IP: ${WORKER2_IP:-NOT FOUND}" >&2
fi

# Generate inventory file
cat > "$INVENTORY_FILE" <<EOF
# Ansible inventory for Kubernetes cluster
# Generated automatically on $(date)
# Master IP: $MASTER_IP
# Worker1 IP: $WORKER1_IP
# Worker2 IP: $WORKER2_IP

[local]
localhost ansible_connection=local

[k8s_master]
k8s-master ansible_host=$MASTER_IP ansible_user=ajasta ansible_become=true

[k8s_workers]
EOF

if [ -n "$WORKER1_IP" ]; then
  cat >> "$INVENTORY_FILE" <<EOF
k8s-worker-1 ansible_host=$WORKER1_IP ansible_user=ajasta ansible_become=true ansible_ssh_common_args='-o ProxyJump=ajasta@$MASTER_IP -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
EOF
fi

if [ -n "$WORKER2_IP" ]; then
  cat >> "$INVENTORY_FILE" <<EOF
k8s-worker-2 ansible_host=$WORKER2_IP ansible_user=ajasta ansible_become=true ansible_ssh_common_args='-o ProxyJump=ajasta@$MASTER_IP -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
EOF
fi

cat >> "$INVENTORY_FILE" <<EOF

[k8s:children]
k8s_master
k8s_workers

[k8s:vars]
ansible_ssh_private_key_file=~/.ssh/id_rsa
ansible_ssh_common_args=-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null -o CheckHostIP=no -o ConnectTimeout=10 -o ServerAliveInterval=5 -o ServerAliveCountMax=3 -o ConnectionAttempts=10
EOF

echo ""
echo "✓ Inventory file created: $INVENTORY_FILE"
echo ""
echo "You can now run:"
echo "  ansible-playbook -i $INVENTORY_FILE other-k8s-playbook.yml"

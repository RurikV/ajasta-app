#!/usr/bin/env bash
# Generate ansible inventory from Yandex Cloud VMs
# This script dynamically discovers k8s-master and all k8s-worker-* VMs
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

# Discover all worker VMs (k8s-worker-*)
echo "Discovering worker nodes..."
WORKERS_JSON=$(yc compute instance list --format json 2>/dev/null | \
  jq -r '[.[] | select(.name | startswith("k8s-worker-")) | {name: .name, ip: .network_interfaces[0].primary_v4_address.one_to_one_nat.address}] | sort_by(.name)')

WORKER_COUNT=$(echo "$WORKERS_JSON" | jq -r 'length')
echo "Found $WORKER_COUNT worker node(s)"

# Build worker IP list for header comments
WORKER_IPS_COMMENT=""
while IFS= read -r worker; do
  WORKER_NAME=$(echo "$worker" | jq -r '.name')
  WORKER_IP=$(echo "$worker" | jq -r '.ip // "NOT_FOUND"')
  echo "  $WORKER_NAME: $WORKER_IP"
  WORKER_IPS_COMMENT="${WORKER_IPS_COMMENT}# ${WORKER_NAME} IP: ${WORKER_IP}\n"
done < <(echo "$WORKERS_JSON" | jq -c '.[]')

# Generate inventory file header
cat > "$INVENTORY_FILE" <<EOF
# Ansible inventory for Kubernetes cluster
# Generated automatically on $(date)
# Master IP: $MASTER_IP
$(echo -e "$WORKER_IPS_COMMENT")
[local]
localhost ansible_connection=local

[k8s_master]
k8s-master ansible_host=$MASTER_IP ansible_user=ajasta ansible_become=true

[k8s_workers]
EOF

# Add all discovered workers to inventory
while IFS= read -r worker; do
  WORKER_NAME=$(echo "$worker" | jq -r '.name')
  WORKER_IP=$(echo "$worker" | jq -r '.ip // empty')
  
  if [ -n "$WORKER_IP" ]; then
    cat >> "$INVENTORY_FILE" <<EOF
$WORKER_NAME ansible_host=$WORKER_IP ansible_user=ajasta ansible_become=true
EOF
  else
    echo "WARNING: Skipping $WORKER_NAME (no public IP found)" >&2
  fi
done < <(echo "$WORKERS_JSON" | jq -c '.[]')

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

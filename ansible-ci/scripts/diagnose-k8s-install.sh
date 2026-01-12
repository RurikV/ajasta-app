#!/bin/bash
# Diagnose Kubernetes installation issues

echo "=========================================="
echo "Kubernetes Installation Diagnostics"
echo "=========================================="
echo ""

cd "$(dirname "$0")/../.."

echo "1. Checking containerd status..."
ansible master-node -i inventory.ini -b -m shell -a "systemctl status containerd --no-pager -l" 2>/dev/null | grep -A 10 "master-node"

echo ""
echo "2. Checking kubelet status..."
ansible master-node -i inventory.ini -b -m shell -a "systemctl status kubelet --no-pager -l" 2>/dev/null | grep -A 10 "master-node"

echo ""
echo "3. Checking if kubeadm was run..."
ansible master-node -i inventory.ini -b -m shell -a "ls -la /etc/kubernetes/" 2>/dev/null | grep "master-node"

echo ""
echo "4. Checking kubeadm init logs..."
ansible master-node -i inventory.ini -b -m shell -a "journalctl -xe --no-pager | tail -50" 2>/dev/null | grep -A 30 "master-node"

echo ""
echo "5. Checking containerd CRI plugin..."
ansible master-node -i inventory.ini -b -m shell -a "ls -la /run/containerd/containerd.sock*" 2>/dev/null | grep "master-node"

echo ""
echo "6. Checking for swap (should be disabled)..."
ansible master-node -i inventory.ini -b -m shell -a "swapon --show" 2>/dev/null | grep "master-node"

echo ""
echo "7. Checking kubelet logs..."
ansible master-node -i inventory.ini -b -m shell -a "journalctl -u kubelet -n 100 --no-pager | tail -50" 2>/dev/null | grep -A 40 "master-node"

echo ""
echo "8. Checking if containers are running..."
ansible master-node -i inventory.ini -b -m shell -a "crictl ps 2>/dev/null || ctr -l || echo 'No containers found'" 2>/dev/null | grep -A 20 "master-node"

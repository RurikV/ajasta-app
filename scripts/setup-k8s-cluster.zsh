#!/usr/bin/env zsh
# Bootstrap a Kubernetes cluster (1 control-plane + N workers) on the provisioned VMs
# Usage:
#   setup-k8s-cluster.zsh <MASTER_PUBLIC_IP> <WORKER_INTERNAL_IPS_CSV>
# Environment:
#   SSH_USERNAME  - SSH user to connect as (default: ajasta)
#   SSH_KEY_FILE  - Path to private SSH key for the user (optional, will use default agent/keys if not set)
#   POD_CIDR      - Pod network CIDR (default: 10.244.0.0/16 for Flannel)
#   K8S_VERSION   - Optional Kubernetes version (e.g., 1.30), installs latest by default
#
# Notes:
# - Master has a public IP; workers are accessible from master over the internal subnet.
# - Script is idempotent: skips init/join if already configured.
# - Installs containerd and kubeadm/kubelet/kubectl using the official repo.
# - Applies Flannel CNI by default.

set -euo pipefail

MASTER_IP=${1:-}
WORKERS_CSV=${2:-}
SSH_USERNAME=${SSH_USERNAME:-ajasta}
SSH_KEY_FILE=${SSH_KEY_FILE:-}
POD_CIDR=${POD_CIDR:-10.244.0.0/16}
K8S_VERSION=${K8S_VERSION:-}

if [ -z "$MASTER_IP" ]; then
  echo "[setup-k8s] MASTER_PUBLIC_IP is required as arg1" >&2
  exit 2
fi

# split CSV into array (allow empty)
WORKER_IPS=()
if [ -n "$WORKERS_CSV" ]; then
  IFS=',' read -rA WORKER_IPS <<< "$WORKERS_CSV"
fi

ssh_common=( -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 )
if [ -n "$SSH_KEY_FILE" ]; then
  ssh_common+=( -i "$SSH_KEY_FILE" )
fi

function rsh() {
  local host=$1; shift
  ssh "${ssh_common[@]}" ${SSH_USERNAME}@${host} "$@"
}

function rscp() {
  local src=$1 dst=$2
  scp "${ssh_common[@]}" "$src" "$dst"
}

# Retry wrapper for SSH commands to handle VM boot/SSH readiness delays
function rsh_retry() {
  local host=$1; shift
  local cmd="$@"
  local attempts=${SSH_RETRY_ATTEMPTS:-10}
  local delay=${SSH_RETRY_DELAY:-5}
  local n=1
  while true; do
    if output=$(ssh "${ssh_common[@]}" ${SSH_USERNAME}@${host} ${cmd} 2>&1); then
      echo "$output"
      return 0
    fi
    if [ $n -ge $attempts ]; then
      echo "[setup-k8s][error] SSH to ${host} failed after ${attempts} attempts. Last error:" >&2
      echo "$output" >&2
      return 1
    fi
    echo "[setup-k8s] SSH to ${host} not ready yet (attempt ${n}/${attempts}). Retrying in ${delay}s..." >&2
    sleep $delay
    n=$(( n + 1 ))
  done
}

# Script that will be executed on each node as root via sudo bash -lc
read -r -d '' REMOTE_NODE_SETUP <<'EOF' || true
set -euo pipefail

# 1) Disable swap (required for kubelet)
if grep -q ' swap ' /proc/swaps 2>/dev/null; then
  swapoff -a || true
fi
# Also remove swap from fstab (idempotent)
sed -ri.bak '/\sswap\s/d' /etc/fstab || true

# 2) Kernel modules and sysctl for Kubernetes networking
modprobe overlay || true
modprobe br_netfilter || true

cat >/etc/modules-load.d/k8s.conf <<MODS
overlay
br_netfilter
MODS

cat >/etc/sysctl.d/99-kubernetes-cri.conf <<SYS
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
SYS
sysctl --system >/dev/null 2>&1 || true

# 3) Install containerd
if ! command -v containerd >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg apt-transport-https software-properties-common
  install -m 0755 -d /etc/apt/keyrings
  if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
  fi
  ARCH=$(dpkg --print-architecture)
  . /etc/os-release
  echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y containerd.io
fi

mkdir -p /etc/containerd
# Remove existing config and regenerate to ensure CRI plugin is enabled
rm -f /etc/containerd/config.toml
containerd config default > /etc/containerd/config.toml
# Ensure SystemdCgroup = true for kubelet compatibility
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml || true
systemctl restart containerd
systemctl enable containerd
# Wait for containerd to be fully ready
sleep 3

# 4) Install kubelet, kubeadm, kubectl
if ! command -v kubeadm >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  if [ ! -f /etc/apt/keyrings/kubernetes-apt-keyring.gpg ]; then
    curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.30/deb/Release.key | \
      gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
    chmod a+r /etc/apt/keyrings/kubernetes-apt-keyring.gpg
  fi
  echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.30/deb/ /' > /etc/apt/sources.list.d/kubernetes.list
  apt-get update -y
  # If K8S_VERSION passed down, honor it; otherwise install latest available in repo
  if [ -n "${K8S_VERSION:-}" ]; then
    apt-get install -y kubelet="${K8S_VERSION}"-* kubeadm="${K8S_VERSION}"-* kubectl="${K8S_VERSION}"-*
  else
    apt-get install -y kubelet kubeadm kubectl
  fi
  apt-mark hold kubelet kubeadm kubectl || true
fi
systemctl enable --now kubelet || true
EOF

# Script to run only on the master
read -r -d '' REMOTE_MASTER_SETUP <<'EOF' || true
set -euo pipefail

POD_CIDR_IN=${POD_CIDR_IN}
SSH_USER_IN=${SSH_USER_IN}

if [ ! -f /etc/kubernetes/admin.conf ]; then
  kubeadm init --pod-network-cidr=${POD_CIDR_IN}
  # set kubeconfig for the SSH user
  mkdir -p /home/${SSH_USER_IN}/.kube
  cp -f /etc/kubernetes/admin.conf /home/${SSH_USER_IN}/.kube/config
  chown -R ${SSH_USER_IN}:${SSH_USER_IN} /home/${SSH_USER_IN}/.kube
  # install Flannel CNI
  sudo -u ${SSH_USER_IN} -H kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/refs/heads/master/Documentation/kube-flannel.yml
else
  echo "[setup-k8s][master] Cluster already initialized, skipping kubeadm init"
fi

# Print a (re)usable join command with token
JOIN_CMD=$(kubeadm token create --print-join-command 2>/dev/null || true)
if [ -z "$JOIN_CMD" ]; then
  # Token may already exist or be expired; create a new one
  JOIN_CMD=$(kubeadm token create --print-join-command)
fi

echo "$JOIN_CMD" > /root/join-worker.sh
chmod +x /root/join-worker.sh
cat /root/join-worker.sh
EOF

# 1) Prepare master node
echo "[setup-k8s] Preparing master ${MASTER_IP}..."
# Use base64 to safely transport the script through SSH
ENCODED_SETUP=$(echo "$REMOTE_NODE_SETUP" | base64)
rsh_retry "$MASTER_IP" "echo '$ENCODED_SETUP' | base64 -d | sudo K8S_VERSION='${K8S_VERSION}' bash -l"

# 2) Initialize control plane (if needed) and fetch join command
echo "[setup-k8s] Initializing control plane (or retrieving join command)..."
ENCODED_MASTER=$(echo "$REMOTE_MASTER_SETUP" | base64)
MASTER_OUT=$(rsh_retry "$MASTER_IP" "echo '$ENCODED_MASTER' | base64 -d | sudo POD_CIDR_IN='${POD_CIDR}' SSH_USER_IN='${SSH_USERNAME}' bash -l")
# Extract the join command line robustly
JOIN_CMD=$(echo "$MASTER_OUT" | grep -E '^kubeadm join' | tail -n1 || true)
if [[ -z "$JOIN_CMD" ]]; then
  # Try to read the join command file explicitly
  JOIN_CMD=$(rsh_retry "$MASTER_IP" "sudo bash -lc 'cat /root/join-worker.sh 2>/dev/null | grep -E ^kubeadm\ join | tail -n1'" || true)
fi
if [[ -z "$JOIN_CMD" ]]; then
  echo "[setup-k8s] Failed to obtain join command from master. Output was:" >&2
  echo "$MASTER_OUT" >&2
  exit 1
fi

echo "[setup-k8s] Join command: $JOIN_CMD"

# 3) For each worker: prepare node and join if not already joined
# Prepare the worker join script (setup + conditional join)
read -r -d '' WORKER_SCRIPT <<WEOF || true
${REMOTE_NODE_SETUP}
if [ ! -f /etc/kubernetes/kubelet.conf ]; then
  ${JOIN_CMD}
else
  echo "[setup-k8s][worker] already joined, skipping"
fi
WEOF

ENCODED_WORKER=$(echo "$WORKER_SCRIPT" | base64)

# SSH options for ProxyJump through master
# We need to explicitly set StrictHostKeyChecking for both the proxy and target
ssh_key_opts=""
if [ -n "$SSH_KEY_FILE" ]; then
  ssh_key_opts="-i $SSH_KEY_FILE"
fi
ssh_proxyjump=( 
  -o StrictHostKeyChecking=no 
  -o UserKnownHostsFile=/dev/null 
  -o GlobalKnownHostsFile=/dev/null
  -o ConnectTimeout=10
  -o ProxyCommand="ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null ${ssh_key_opts} -W %h:%p ${SSH_USERNAME}@${MASTER_IP}"
)
if [ -n "$SSH_KEY_FILE" ]; then
  ssh_proxyjump+=( -i "$SSH_KEY_FILE" )
fi

function rsh_worker_retry() {
  local worker_ip=$1; shift
  local cmd="$@"
  local attempts=${SSH_RETRY_ATTEMPTS:-10}
  local delay=${SSH_RETRY_DELAY:-5}
  local n=1
  while true; do
    if output=$(ssh "${ssh_proxyjump[@]}" ${SSH_USERNAME}@${worker_ip} ${cmd} 2>&1); then
      echo "$output"
      return 0
    fi
    if [ $n -ge $attempts ]; then
      echo "[setup-k8s][error] SSH to worker ${worker_ip} via master failed after ${attempts} attempts. Last error:" >&2
      echo "$output" >&2
      return 1
    fi
    echo "[setup-k8s] SSH to worker ${worker_ip} not ready yet (attempt ${n}/${attempts}). Retrying in ${delay}s..." >&2
    sleep $delay
    n=$(( n + 1 ))
  done
}

for WIP in "${WORKER_IPS[@]}"; do
  [ -z "$WIP" ] && continue
  echo "[setup-k8s] Preparing worker ${WIP}..."
  rsh_worker_retry "$WIP" "echo '$ENCODED_WORKER' | base64 -d | sudo K8S_VERSION='${K8S_VERSION}' bash -l"
done

echo "[setup-k8s] Kubernetes cluster is ready (control-plane + ${#WORKER_IPS[@]} worker(s))."

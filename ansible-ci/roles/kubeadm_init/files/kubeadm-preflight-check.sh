#!/bin/bash
# Pre-flight checks for kubeadm init
set -euo pipefail

EXIT_CODE=0

echo "=== Pre-flight Checks for kubeadm init ==="

# Check 1: Memory (minimum 2GB recommended)
MEM_MB=$(free -m | awk '/^Mem:/ {print $2}')
echo "Available memory: ${MEM_MB}MB"
if [ "$MEM_MB" -lt 1800 ]; then
  echo "WARNING: Less than 2GB memory available. Kubernetes may fail to start." >&2
  EXIT_CODE=1
fi

# Check 2: Disk space for /var/lib/kubelet (minimum 10GB free)
DISK_GB=$(df -BG /var/lib 2>/dev/null | awk 'NR==2 {gsub(/G/,"",$4); print $4}')
echo "Available disk space on /var/lib: ${DISK_GB}GB"
if [ "$DISK_GB" -lt 10 ]; then
  echo "WARNING: Less than 10GB free on /var/lib. May cause image pull or pod startup issues." >&2
  EXIT_CODE=1
fi

# Check 3: Port 6443 should NOT be in use
if ss -tlnp 2>/dev/null | grep -q ':6443' || netstat -tlnp 2>/dev/null | grep -q ':6443'; then
  echo "ERROR: Port 6443 is already in use!" >&2
  ss -tlnp 2>/dev/null | grep ':6443' || netstat -tlnp 2>/dev/null | grep ':6443' >&2
  EXIT_CODE=1
else
  echo "Port 6443: Available"
fi

# Check 4: Port 2379 (etcd) should NOT be in use (unless etcd already running)
if ss -tlnp 2>/dev/null | grep -q ':2379' || netstat -tlnp 2>/dev/null | grep -q ':2379'; then
  # This is okay if etcd container is already running from previous init
  echo "Port 2379: Already in use (etcd may be running from previous init)"
else
  echo "Port 2379: Available"
fi

# Check 5: Required directories writable
for dir in /etc/kubernetes /var/lib/kubelet /etc/cni; do
  if [ ! -d "$dir" ]; then
    mkdir -p "$dir" 2>/dev/null || {
      echo "ERROR: Cannot create directory $dir" >&2
      EXIT_CODE=1
    }
  fi
  if [ ! -w "$dir" ]; then
    echo "ERROR: Directory $dir is not writable" >&2
    EXIT_CODE=1
  else
    echo "Directory $dir: Writable"
  fi
done

# Check 6: Container runtime CRI socket accessible
# NOTE: kubeadm doesn't need crictl, it needs the CRI socket!
# Verify containerd CRI plugin is responding via socket
CRI_SOCKET="/run/containerd/containerd.sock"
if [ ! -S "$CRI_SOCKET" ]; then
  echo "ERROR: CRI socket not found at $CRI_SOCKET" >&2
  echo "containerd may not be running or CRI plugin not enabled" >&2
  EXIT_CODE=1
elif ! timeout 5 ctr plugins ls 2>/dev/null | grep -E "io.containerd.(grpc.v1.*cri|cri.v1)" | grep -q "cri"; then
  echo "ERROR: CRI plugin not loaded in containerd" >&2
  echo "containerd plugins:" >&2
  ctr plugins ls 2>&1 | head -10 >&2
  EXIT_CODE=1
else
  echo "CRI socket: Accessible at $CRI_SOCKET"
  echo "CRI plugin: Loaded and responsive"
fi

echo "=== Pre-flight checks complete ==="
exit $EXIT_CODE

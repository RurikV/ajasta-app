#!/bin/bash
# Fix containerd CRI plugin configuration
set -euo pipefail

echo "=== Fixing containerd CRI plugin ==="

# Check if CRI plugin is disabled
echo "Checking containerd config..."
if grep -q "disabled_plugins.*cri" /etc/containerd/config.toml 2>/dev/null; then
  echo "✗ CRI plugin is DISABLED in config"
  echo "Enabling CRI plugin..."

  # Remove disabled_plugins line or set it to empty
  sed -i 's/^disabled_plugins.*$/disabled_plugins = []/' /etc/containerd/config.toml
  echo "✓ Enabled CRI plugin"
else
  echo "✓ CRI plugin not explicitly disabled"
fi

# Ensure CRI plugin is in the config (for newer containerd versions)
if ! grep -q "cri" /etc/containerd/config.toml 2>/dev/null; then
  echo "CRI plugin not found in config, ensuring it's enabled..."

  # Add CRI to plugins list if version has it
  if grep -q "^\[plugins\]" /etc/containerd/config.toml 2>/dev/null; then
    # Add CRI plugin after [plugins] section
    sed -i '/^\[plugins\]/a \  [plugins."io.containerd.grpc.v1.cri"\n\troot = "/var/spool/containerd/io.containerd.grpc.v1.cri"\n' /etc/containerd/config.toml
  else
    echo "No [plugins] section, containerd might be old version"
  fi
fi

# Check if sandbox_image is set
if ! grep -q "sandbox_image" /etc/containerd/config.toml 2>/dev/null; then
  echo "Adding sandbox_image to config..."
  # Add sandbox_image to [plugins."io.containerd.grpc.v1.cri".containerd] section
  if grep -q 'root = "/var/spool/containerd/io.containerd.grpc.v1.cri"' /etc/containerd/config.toml 2>/dev/null; then
    sed -i '/root = "\/var\/spool\/containerd\/io.containerd.grpc.v1.cri"/a \  \[plugins."io.containerd.grpc.v1.cri".containerd\]\n    sandbox_image = "registry.k8s.io/pause:3.10"\n' /etc/containerd/config.toml
  fi
fi

echo "Restarting containerd to apply changes..."
systemctl restart containerd

echo "Waiting for containerd to start..."
sleep 5

# Wait for socket
echo "Waiting for CRI socket..."
for i in {1..30}; do
  if [ -S /run/containerd/containerd.sock ]; then
    echo "✓ CRI socket is ready"
    break
  fi
  echo "Waiting... ($i/30)"
  sleep 2
done

# Verify CRI plugin is loaded
echo "Verifying CRI plugin..."
if ctr plugins ls 2>/dev/null | grep -E "io.containerd.(grpc.v1.*cri|cri.v1)" | grep -q "cri"; then
  echo "✓ CRI plugin is loaded!"
  ctr plugins ls | grep -i cri
  exit 0
else
  echo "✗ CRI plugin still NOT loaded!"
  echo "Plugins loaded:"
  ctr plugins ls
  exit 1
fi

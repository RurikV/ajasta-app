#!/usr/bin/env zsh
# Example driver to provision Yandex Cloud resources using zsh scripts
# - Creates external and internal networks/subnets
# - Creates a service account and access key
# - Reserves a static public IP
# - Creates a ajasta VM with the reserved static IP (single NIC on external subnet)
# Note: Security groups and multi-NIC configuration are not included in example scripts.

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR"

require_cmd() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || { echo "Missing command: $c" >&2; exit 1; }; done; }
require_cmd zsh yc jq

# Create external network + subnet
YC_NETWORK_NAME=external-ajasta-network \
YC_SUBNET_NAME=ajasta-external-segment \
YC_SUBNET_RANGE=172.16.17.0/28 \
./create-network.zsh

# Create internal network + subnet
YC_NETWORK_NAME=internal-ajasta-network \
YC_SUBNET_NAME=ajasta-internal-segment \
YC_SUBNET_RANGE=172.16.16.0/24 \
./create-network.zsh

# Create service account and keys
YC_SA_NAME=otus ./create-sa.zsh

# Reserve a static IP for ajasta
ajasta_IP=$(YC_ADDRESS_NAME=ajasta-static-ip ./create-static-ip.zsh)
echo "ajasta static IP: $ajasta_IP"

# Create ajasta VM (single NIC on external subnet) and inject your SSH public key
# Ensure we have a public key; generate if missing
KEY_PUB=${SSH_PUBKEY_FILE:-./ajasta_ed25519.pub}
if [ ! -f "$KEY_PUB" ]; then
  echo "SSH public key '$KEY_PUB' not found. Generating a new ed25519 keypair..."
  KEY_PUB=$(zsh ./generate-ssh-key.zsh ./ajasta_ed25519 ajasta@ajasta-host)
  echo "Generated keypair. Public key: $KEY_PUB"
fi

YC_SUBNET_NAME=ajasta-external-segment \
METADATA_YAML=./metadata.yaml \
SSH_USERNAME=${SSH_USERNAME:-ajasta} \
SSH_PUBKEY_FILE="$KEY_PUB" \
./create-vm-static-ip.zsh ajasta-host

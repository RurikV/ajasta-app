#!/usr/bin/env zsh
# Simple SSH to ajasta-host (single attempt)
# Usage:
#   ./ssh-ajasta.zsh [COMMAND]
# Env:
#   ajasta_VM_NAME: VM name to resolve public IP (default: ajasta-host)
#   SSH_HOST: optional host/IP override
#   SSH_USER: SSH username (default: ajasta)
#   SSH_KEY: private key path (default: ./ajasta_ed25519)
#   SSH_PORT: SSH port (default: 22)
#   SSH_OPTS: extra ssh options
# Notes:
#   - This script performs exactly one SSH attempt. It does not modify VM metadata or retry.

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq ssh

ajasta_VM_NAME=${ajasta_VM_NAME:-ajasta-host}
SSH_USER=${SSH_USER:-ajasta}
SSH_KEY=${SSH_KEY:-./ajasta_ed25519}
SSH_PORT=${SSH_PORT:-22}
CMD=${1:-"echo Connected as $(whoami) on $(hostname)"}

# Resolve host
if [ -n "${SSH_HOST:-}" ]; then
  HOST="$SSH_HOST"
else
  log "Resolving public IP for instance '$ajasta_VM_NAME'..."
  HOST=$(yc compute instance get --name "$ajasta_VM_NAME" --format json |
    jq -r '.network_interfaces[].primary_v4_address.one_to_one_nat.address' |
    grep -E '^[0-9]+(\.[0-9]+){3}$' | head -n1)
  if [ -z "$HOST" ]; then
    echo "Error: could not resolve public IP for VM '$ajasta_VM_NAME'" >&2
    exit 1
  fi
fi

# Validate key
if [ ! -f "$SSH_KEY" ]; then
  echo "Error: SSH key '$SSH_KEY' not found. Generate it with: zsh ./generate-ssh-key.zsh" >&2
  exit 1
fi

# One SSH attempt
log "Connecting to $SSH_USER@$HOST (port $SSH_PORT) ..."
set +e
ssh -i "$SSH_KEY" \
    -p "$SSH_PORT" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    ${=SSH_OPTS:-} \
    "$SSH_USER@$HOST" "$CMD"
rc=$?
set -e

if [ $rc -eq 0 ]; then
  log "SSH connection succeeded to $SSH_USER@$HOST."
  exit 0
fi

# Helpful guidance (no retries)
echo "SSH connection failed (exit $rc)." >&2
echo "Hints:" >&2
echo " - Ensure the VM has your public key authorized for user '$SSH_USER'." >&2
echo " - If needed, run: SSH_USERNAME=$SSH_USER SSH_PUBKEY_FILE=./ajasta_ed25519.pub zsh ./add-ssh-key.zsh $ajasta_VM_NAME" >&2
echo " - Then rerun: ./ssh-ajasta.zsh" >&2
exit $rc

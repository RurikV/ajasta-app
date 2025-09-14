#!/usr/bin/env zsh
# Add or append an SSH public key to an existing YC VM's metadata (ssh-keys)
# Usage:
#   SSH_PUBKEY_FILE=~/.ssh/id_rsa.pub SSH_USERNAME=ubuntu ./add-ssh-key.zsh <vm_name>
# or
#   SSH_PUBKEY="ssh-ed25519 AAAA... comment" SSH_USERNAME=ubuntu ./add-ssh-key.zsh <vm_name>
# Notes:
# - Default SSH_USERNAME is 'ubuntu' (works for Ubuntu images). Ensure the user exists on the VM.
# - This script preserves existing ssh-keys and appends a new line if not already present.

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq grep

if [ ${#} -lt 1 ]; then
  echo "Usage: $0 <vm_name>" >&2
  exit 2
fi

vm_name="$1"
SSH_USERNAME=${SSH_USERNAME:-ubuntu}

# Load pubkey
if [ -z "${SSH_PUBKEY_FILE:-}" ] && [ -z "${SSH_PUBKEY:-}" ]; then
  echo "Error: provide SSH_PUBKEY_FILE or SSH_PUBKEY" >&2
  exit 2
fi
if [ -n "${SSH_PUBKEY_FILE:-}" ]; then
  if [ ! -f "$SSH_PUBKEY_FILE" ]; then
    echo "Error: SSH_PUBKEY_FILE '$SSH_PUBKEY_FILE' not found" >&2
    exit 1
  fi
  PUBKEY_CONTENT=$(cat "$SSH_PUBKEY_FILE")
else
  PUBKEY_CONTENT="$SSH_PUBKEY"
fi

NEW_ENTRY="${SSH_USERNAME}:${PUBKEY_CONTENT}"

# Fetch existing ssh-keys metadata (may be empty)
EXISTING=$(yc compute instance get --name "$vm_name" --format json | jq -r '.metadata["ssh-keys"] // ""') || EXISTING=""

# If already present, exit gracefully
if [ -n "$EXISTING" ] && print -r -- "$EXISTING" | grep -Fqx -- "$NEW_ENTRY"; then
  log "SSH key for user '$SSH_USERNAME' already present on instance '$vm_name'."
  exit 0
fi

NEW_VALUE="${EXISTING}"
if [ -n "$NEW_VALUE" ]; then
  case "$NEW_VALUE" in
    *$'\n') NEW_VALUE+="$NEW_ENTRY" ;;
    *) NEW_VALUE+=$'\n'"$NEW_ENTRY" ;;
  esac
else
  NEW_VALUE="$NEW_ENTRY"
fi

log "Updating instance '$vm_name' metadata ssh-keys (appending new key)..."
yc compute instance update \
  --name "$vm_name" \
  --metadata "ssh-keys=$NEW_VALUE" >/dev/null

log "SSH key added. You can now SSH as '$SSH_USERNAME'."

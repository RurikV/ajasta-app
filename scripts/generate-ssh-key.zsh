#!/usr/bin/env zsh
# Generate an ed25519 SSH keypair for connecting to ajasta
# Usage:
#   ./generate-ssh-key.zsh [KEY_PATH] [COMMENT]
# Defaults:
#   KEY_PATH: ./ajasta_ed25519
#   COMMENT: ajasta-key
# Notes:
# - No passphrase is set (non-interactive automation). You can change this as needed.

set -euo pipefail

KEY_PATH=${1:-./ajasta_ed25519}
COMMENT=${2:-ajasta-key}

if [ -f "$KEY_PATH" ] || [ -f "$KEY_PATH.pub" ]; then
  echo "Key or public key already exists at $KEY_PATH(.pub). Skipping generation." >&2
  echo "$KEY_PATH.pub"
  exit 0
fi

mkdir -p "$(dirname "$KEY_PATH")"
ssh-keygen -t ed25519 -f "$KEY_PATH" -C "$COMMENT" -N '' -q
chmod 600 "$KEY_PATH"
chmod 644 "$KEY_PATH.pub"

echo "$KEY_PATH.pub"

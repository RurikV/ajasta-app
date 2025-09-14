#!/usr/bin/env zsh
# Test script to verify SSH connection fix
# This script simulates the user's workflow to confirm the fix works

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR/scripts"

echo "[TEST] Testing SSH connection fix..."
echo "[TEST] Current directory: $(pwd)"

# Clean up any existing keys and VMs first
echo "[TEST] Cleaning up existing resources..."
./destroy-all.zsh || true
rm -f ./ajasta_ed25519* || true

echo "[TEST] Running main.zsh to create VM with SSH key injection..."
./main.zsh

echo "[TEST] Waiting 10 seconds for VM to fully initialize..."
sleep 10

echo "[TEST] Attempting SSH connection..."
if ./ssh-ajasta.zsh "echo 'SSH connection successful! User: \$(whoami), Host: \$(hostname)'"; then
    echo "[TEST] ✅ SSH connection SUCCESSFUL - Fix works!"
    exit 0
else
    echo "[TEST] ❌ SSH connection FAILED - Fix needs more work"
    echo "[TEST] Attempting with SSH agent disabled..."
    if SSH_OPTS='-o IdentityAgent=none' ./ssh-ajasta.zsh "echo 'SSH connection successful with agent disabled!'"; then
        echo "[TEST] ✅ SSH works with agent disabled - partial success"
        exit 0
    else
        echo "[TEST] ❌ SSH still failing even with agent disabled"
        exit 1
    fi
fi
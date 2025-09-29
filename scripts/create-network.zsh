#!/usr/bin/env zsh
# Create a VPC network and subnet in Yandex Cloud (idempotent)
# Usage:
#   YC_NETWORK_NAME=<name> YC_SUBNET_NAME=<name> YC_SUBNET_RANGE=<cidr> [YC_ZONE=ru-central1-b] ./create-network.zsh
# Defaults (override via env):
#   YC_NETWORK_NAME: external-ajasta-network
#   YC_SUBNET_NAME:  ajasta-external-segment
#   YC_SUBNET_RANGE: 172.16.17.0/28

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq

YC_NETWORK_NAME=${YC_NETWORK_NAME:-external-ajasta-network}
YC_SUBNET_NAME=${YC_SUBNET_NAME:-ajasta-external-segment}
YC_SUBNET_RANGE=${YC_SUBNET_RANGE:-172.16.17.0/28}

# Check if network exists, create if not
if network_exists "$YC_NETWORK_NAME"; then
  log "Network '$YC_NETWORK_NAME' already exists, reusing it."
else
  log "Creating network '$YC_NETWORK_NAME' in folder $YC_FOLDER_ID..."
  yc vpc network create \
    --name "$YC_NETWORK_NAME" \
    --folder-id "$YC_FOLDER_ID"
  log "Network '$YC_NETWORK_NAME' created."
fi

# Check if subnet exists, create if not
if subnet_exists "$YC_SUBNET_NAME"; then
  log "Subnet '$YC_SUBNET_NAME' already exists, reusing it."
else
  log "Creating subnet '$YC_SUBNET_NAME' ($YC_SUBNET_RANGE) in zone $YC_ZONE..."
  yc vpc subnet create \
    --name "$YC_SUBNET_NAME" \
    --zone "$YC_ZONE" \
    --range "$YC_SUBNET_RANGE" \
    --network-name "$YC_NETWORK_NAME" \
    --folder-id "$YC_FOLDER_ID"
  log "Subnet '$YC_SUBNET_NAME' created."
fi

log "Network '$YC_NETWORK_NAME' and subnet '$YC_SUBNET_NAME' are ready."

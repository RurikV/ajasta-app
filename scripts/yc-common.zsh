# Common helpers for Yandex Cloud zsh scripts
# shellcheck disable=SC2154

# Use zsh options for safer scripting
set -euo pipefail

# Define logger early so it can be used during env loading
log() {
  local ts
  ts=$(date '+%Y-%m-%d %H:%M:%S')
  echo "[$ts] $*"
}

# Load environment from .env (optional) before applying defaults
# You can set ENV_FILE to point to a custom env file path
ENV_FILE_PATH=${ENV_FILE:-${SCRIPT_DIR:-.}/.env}
if [ -f "$ENV_FILE_PATH" ]; then
  # Export variables defined in the env file
  # The file can contain either KEY=VALUE or export KEY=VALUE lines
  log "Loading environment from $ENV_FILE_PATH"
  set -a
  source "$ENV_FILE_PATH"
  set +a
fi

# Defaults (can be overridden via environment or .env)
export YC_CLOUD_ID=${YC_CLOUD_ID:-b1g8j62dkk1c16b1499h}
export YC_FOLDER_ID=${YC_FOLDER_ID:-b1gndaq08b52358dleke}
export YC_ZONE=${YC_ZONE:-ru-central1-b}

require_cmd() {
  for c in "$@"; do
    if ! command -v "$c" >/dev/null 2>&1; then
      echo "Error: required command '$c' not found in PATH" >&2
      exit 1
    fi
  done
}

# Check existence helpers (return 0 if exists)
network_exists() { yc vpc network get --name "$1" >/dev/null 2>&1; }
subnet_exists() { yc vpc subnet get --name "$1" >/dev/null 2>&1; }
sa_exists() { yc iam service-account get --name "$1" >/dev/null 2>&1; }
address_exists() { yc vpc address get --name "$1" >/dev/null 2>&1; }
instance_exists() { yc compute instance get --name "$1" >/dev/null 2>&1; }

# Delete helpers (best-effort, ignore if not exists)
delete_subnet_by_name() {
  local name="$1"
  if subnet_exists "$name"; then
    log "Deleting subnet '$name'..."
    yc vpc subnet delete --name "$name" || true
  fi
}

delete_network_by_name() {
  local name="$1"
  if network_exists "$name"; then
    log "Deleting network '$name'..."
    yc vpc network delete --name "$name" || true
  fi
}

delete_sa_by_name() {
  local name="$1"
  if sa_exists "$name"; then
    log "Deleting service account '$name'..."
    yc iam service-account delete --name "$name" || true
  fi
}

delete_address_by_name() {
  local name="$1"
  if address_exists "$name"; then
    log "Deleting static address '$name'..."
    yc vpc address delete --name "$name" || true
  fi
}

delete_instance_by_name() {
  local name="$1"
  if instance_exists "$name"; then
    log "Deleting instance '$name'..."
    yc compute instance delete --name "$name" || true
  fi
}

wait_instance_absent() {
  local name="$1"; local attempts=${2:-60}; local sleep_s=${3:-5}
  local i=0
  while [ $i -lt $attempts ]; do
    if ! instance_exists "$name"; then
      return 0
    fi
    sleep "$sleep_s"
    i=$((i+1))
  done
  log "Warning: instance '$name' still present after wait."
  return 1
}

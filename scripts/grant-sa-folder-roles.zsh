#!/usr/bin/env zsh
# Grant minimal required roles on a Yandex Cloud folder to a Service Account used by Terraform
#
# New simplified AUTO mode (no args):
#   scripts/grant-sa-folder-roles.zsh
#   - Reads folder_id and other hints from terraform/terraform.tfvars
#   - Tries to resolve Service Account from:
#       1) yc_service_account_key_file -> .service_account_id
#       2) tfvars yc_sa_name -> SA id via `yc iam service-account get`
#       3) default SA name: ajasta-tf
#   - Grants roles: vpc.admin, vpc.publicAdmin, compute.editor
#
# Flag-based usage (overrides auto):
#   scripts/grant-sa-folder-roles.zsh [--folder-id ID] [--sa-id SA_ID | --sa-name SA_NAME] \
#                                     [--roles r1,r2,...] [--tfvars PATH] [--key-file PATH] [--dry-run]
#
# Backward-compatible legacy usage:
#   scripts/grant-sa-folder-roles.zsh <folder-id> <sa-name|sa-id> [role1 role2 ...]
#
# Notes:
# - Run this with a human identity that can manage access bindings on the folder (yc init).
# - If `vpc.publicAdmin` cannot be granted, use ephemeral NAT IPs in Terraform (ask maintainer to toggle).

set -euo pipefail

SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)

require_cmd() {
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 || { echo "Error: required command '$c' not found in PATH" >&2; exit 1; }
  done
}

usage() {
  cat >&2 <<USAGE
Usage:
  $0                      # auto-detect from terraform/terraform.tfvars and key JSON
  $0 --folder-id ID [--sa-id SA_ID | --sa-name SA_NAME] [--roles r1,r2,...] [--tfvars PATH] [--key-file PATH] [--dry-run]
  $0 <folder-id> <sa-name|sa-id> [role1 role2 ...]  # legacy positional mode
USAGE
}

log() { echo "[grant-sa-folder-roles] $*"; }

require_cmd yc jq

# Defaults
TFVARS_DEFAULT="$SCRIPT_DIR/../terraform/terraform.tfvars"
KEY_DEFAULT="$SCRIPT_DIR/../sa-iam-key.json"
DEFAULT_ROLES=(vpc.admin vpc.publicAdmin compute.editor)

# Parse flags
FOLDER_ID=""
SA_ID=""
SA_NAME=""
ROLES_CSV=""
TFVARS_PATH="$TFVARS_DEFAULT"
KEY_FILE=""
DRY_RUN=false

if [[ ${#@} -gt 0 && "$1" != --* && ${#@} -ge 2 ]]; then
  # Legacy positional mode
  FOLDER_ID="$1"; SA_NAME="$2"; shift 2 || true
  # Remaining args interpreted as roles (space-separated)
  if [[ ${#@} -gt 0 ]]; then
    ROLES=("$@")
  else
    ROLES=(${DEFAULT_ROLES[@]})
  fi
else
  # Flag mode or auto mode
  while [[ ${#@} -gt 0 ]]; do
    case "$1" in
      --folder-id) FOLDER_ID="${2:-}"; shift 2;;
      --sa-id) SA_ID="${2:-}"; shift 2;;
      --sa-name) SA_NAME="${2:-}"; shift 2;;
      --roles) ROLES_CSV="${2:-}"; shift 2;;
      --tfvars) TFVARS_PATH="${2:-}"; shift 2;;
      --key-file) KEY_FILE="${2:-}"; shift 2;;
      --dry-run) DRY_RUN=true; shift;;
      -h|--help) usage; exit 2;;
      *) echo "Unknown argument: $1" >&2; usage; exit 2;;
    esac
  done
  # Build ROLES array
  if [[ -n "$ROLES_CSV" ]]; then
    IFS=',' read -rA ROLES <<< "$ROLES_CSV"
  else
    ROLES=(${DEFAULT_ROLES[@]})
  fi
fi

# Ensure YC CLI is initialized
if ! yc config list >/dev/null 2>&1; then
  echo "YC CLI is not initialized. Run 'yc init' first." >&2
  exit 1
fi

# Helper: parse quoted value of a key from tfvars (simple HCL line: key = "value")
parse_tfvar() {
  local key="$1" file="$2"
  [[ -f "$file" ]] || return 1
  # Strip commented lines, take the last assignment, extract the quoted value
  # Works on macOS/BSD and GNU tools.
  # Example line: yc_folder_id = "b1g..."
  local line
  line=$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "$file" | sed -E '/^[[:space:]]*#/d' | tail -n1 || true)
  if [[ -z "$line" ]]; then
    return 1
  fi
  # Extract text between the first pair of double quotes (portable for BSD/GNU sed)
  echo "$line" | sed -E 's/^[^=]*=[[:space:]]*"([^"]*)".*/\1/'
}

# Resolve folder id
if [[ -z "$FOLDER_ID" ]]; then
  if [[ -f "$TFVARS_PATH" ]]; then
    FOLDER_ID=$(parse_tfvar yc_folder_id "$TFVARS_PATH" || true)
  fi
fi
if [[ -z "$FOLDER_ID" ]]; then
  FOLDER_ID=$(yc config get folder-id 2>/dev/null || true)
fi
if [[ -z "$FOLDER_ID" ]]; then
  echo "Failed to resolve folder id. Provide --folder-id or set yc_folder_id in $TFVARS_PATH or configure yc CLI." >&2
  exit 2
fi

# Resolve Service Account ID
if [[ -z "$SA_ID" ]]; then
  # Try key file first
  if [[ -z "$KEY_FILE" ]]; then
    # from tfvars
    if [[ -f "$TFVARS_PATH" ]]; then
      KEY_FILE=$(parse_tfvar yc_service_account_key_file "$TFVARS_PATH" || true)
      # Make it relative to repo root if it starts with ../ from terraform dir
      if [[ -n "$KEY_FILE" && "$KEY_FILE" == ../* ]]; then
        # terraform dir is SCRIPT_DIR/../terraform
        local_base="$SCRIPT_DIR/.."
        KEY_FILE="$local_base/${KEY_FILE#../}"
      fi
    fi
    # If still empty, try default key in repo root
    if [[ -z "$KEY_FILE" && -f "$KEY_DEFAULT" ]]; then
      KEY_FILE="$KEY_DEFAULT"
    fi
  fi
  if [[ -n "$KEY_FILE" && -f "$KEY_FILE" ]]; then
    SA_ID=$(jq -r '.service_account_id // empty' "$KEY_FILE" || true)
  fi
fi

if [[ -z "$SA_ID" && -n "$SA_NAME" ]]; then
  SA_ID=$(yc iam service-account get --name "$SA_NAME" --format json | jq -r '.id // empty' || true)
fi

if [[ -z "$SA_ID" && -z "$SA_NAME" ]]; then
  # Try tfvars yc_sa_name
  if [[ -f "$TFVARS_PATH" ]]; then
    SA_NAME=$(parse_tfvar yc_sa_name "$TFVARS_PATH" || true)
  fi
fi

if [[ -z "$SA_ID" && -n "$SA_NAME" ]]; then
  SA_ID=$(yc iam service-account get --name "$SA_NAME" --format json | jq -r '.id // empty' || true)
fi

if [[ -z "$SA_ID" && -z "$SA_NAME" ]]; then
  # Fall back to default SA name
  SA_NAME="ajasta-tf"
  SA_ID=$(yc iam service-account get --name "$SA_NAME" --format json | jq -r '.id // empty' || true)
fi

if [[ -z "$SA_ID" ]]; then
  echo "Failed to resolve Service Account id. Provide --sa-id or --sa-name, or ensure key JSON exists (yc_service_account_key_file) or SA 'ajasta-tf' is present." >&2
  exit 2
fi

log "Folder: $FOLDER_ID"
log "Service Account ID: $SA_ID${SA_NAME:+ (name hint: $SA_NAME)}"
log "Roles to ensure: ${ROLES[*]}"

if $DRY_RUN; then
  log "Dry-run requested; exiting before changes."
  exit 0
fi

# Fetch current bindings once (list existing access bindings)
current_bindings_json=$(yc resource-manager folder list-access-bindings "$FOLDER_ID" --format json)

for role in "${ROLES[@]}"; do
  have=$(echo "$current_bindings_json" | jq -e --arg r "$role" --arg sa "$SA_ID" '
    .bindings[]? | select(.role_id==$r and (.subject.type=="serviceAccount") and (.subject.id==$sa))') || true
  if [[ -n "$have" ]]; then
    log "Already has role $role"
    continue
  fi
  log "Adding role $role..."
  yc resource-manager folder add-access-binding "$FOLDER_ID" \
    --role "$role" \
    --subject serviceAccount:"$SA_ID" >/dev/null
  log "Granted $role"
done

log "Done."

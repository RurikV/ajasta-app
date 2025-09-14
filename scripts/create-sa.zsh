#!/usr/bin/env zsh
# Create a Service Account (SA), assign roles, and create a static access key (idempotent).
# Why a Service Account?
# - Principle of least privilege: grant the VM only the YC permissions it needs (e.g., compute.admin,
#   storage.*) instead of using your personal identity.
# - Non-human identity: lets software on the VM call YC APIs securely without embedding your user creds.
# - Access keys: enable programmatic access to services like Object Storage (S3-compatible) if needed.
# Usage:
#   YC_SA_NAME=otus ./create-sa.zsh
# Output: writes sa-key.json with access and secret keys.

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq

YC_SA_NAME=${YC_SA_NAME:-otus}
SA_KEY_FILE=${SA_KEY_FILE:-sa-key.json}

log "Ensuring service account '$YC_SA_NAME' is absent (to recreate fresh)..."
delete_sa_by_name "$YC_SA_NAME" || true
[ -f "$SA_KEY_FILE" ] && { log "Removing old $SA_KEY_FILE"; rm -f "$SA_KEY_FILE"; }

log "Creating service account '$YC_SA_NAME'..."
SA_JSON=$(yc iam service-account create \
  --name "$YC_SA_NAME" \
  --description "Service account for VM" \
  --format json)
log "Service account created"

log "Getting service account ID..."
YC_SA_ID=$(echo "$SA_JSON" | jq -r '.id')

# Assign roles at folder scope
typeset -a ROLES
ROLES=(
  storage.viewer
  storage.uploader
  storage.admin
  compute.admin
)

for role in "${ROLES[@]}"; do
  log "Assigning role ${role} to SA ${YC_SA_ID} in folder ${YC_FOLDER_ID}..."
  yc resource-manager folder add-access-binding \
    --id "$YC_FOLDER_ID" \
    --role "$role" \
    --subject serviceAccount:"$YC_SA_ID" \
    --async
  log "Role ${role} assigned"
done

log "Creating static access key for SA..."
YC_SA_KEY=$(yc iam access-key create \
  --service-account-id "$YC_SA_ID" \
  --description "Static access key for service account" \
  --format json)

echo "$YC_SA_KEY" | jq . > "$SA_KEY_FILE"
log "Service account ID: $YC_SA_ID"
log "Service account created successfully!"
log "Static access key created and saved to $SA_KEY_FILE"

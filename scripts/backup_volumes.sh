#!/usr/bin/env bash
# Backup Docker named volumes used by this project.
# Creates compressed tarballs in ./backups by default.
# Usage examples:
#   ./scripts/backup_volumes.sh                 # backup default volumes
#   VOLUMES="ajasta_pg_data" ./scripts/backup_volumes.sh
#   BACKUP_DIR=/var/backups ./scripts/backup_volumes.sh

set -euo pipefail

# Default config
VOLUMES=${VOLUMES:-"ajasta_pg_data"}
BACKUP_DIR=${BACKUP_DIR:-"./backups"}
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

mkdir -p "${BACKUP_DIR}"

echo "[backup] Backing up volumes: ${VOLUMES}"
for VOL in ${VOLUMES}; do
  ARCHIVE_NAME="${VOL}-${TIMESTAMP}.tar.gz"
  echo "[backup] Creating ${BACKUP_DIR}/${ARCHIVE_NAME}"
  # Use a lightweight container to read the volume and create a tar.gz archive
  docker run --rm \
    -v "${VOL}:/volume:ro" \
    -v "$(pwd)/${BACKUP_DIR}:/backup" \
    alpine:3.20 sh -c "apk add --no-cache tar >/dev/null && cd /volume && tar -czf /backup/${ARCHIVE_NAME} ."
  echo "[backup] Saved ${BACKUP_DIR}/${ARCHIVE_NAME}"

done

echo "[backup] Done. Files stored under ${BACKUP_DIR}"

# Restore hint
cat <<EOF
[restore]
To restore a volume from an archive:
  1) Stop services using the volume.
  2) Create the (empty) volume if not exists: docker volume create <VOLUME>
  3) Import the archive into the volume:
     docker run --rm \
       -v <VOLUME>:/volume \
       -v /path/to/backups:/backup \
       alpine:3.20 sh -c "apk add --no-cache tar >/dev/null && cd /volume && tar -xzf /backup/<ARCHIVE>.tar.gz"
  4) Start services again.
EOF

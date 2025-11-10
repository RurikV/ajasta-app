#!/usr/bin/env bash
#
# update-kubeconfig-after-ip-change.sh
# Automated script to update kubeconfig after Kubernetes master IP changes
#
# Usage: 
#   ./scripts/update-kubeconfig-after-ip-change.sh <new-master-ip>
#
# Example:
#   ./scripts/update-kubeconfig-after-ip-change.sh 89.169.183.199
#

set -euo pipefail

# Check arguments
if [ $# -ne 1 ]; then
    echo "Usage: $0 <new-master-ip>"
    echo "Example: $0 89.169.183.199"
    exit 1
fi

NEW_MASTER_IP="$1"
SSH_USER="${SSH_USER:-ajasta}"
KUBE_CONFIG_PATH="${HOME}/.kube/config"

echo "🔄 Updating kubeconfig for new master IP: ${NEW_MASTER_IP}"
echo "================================================"
echo ""

# 1) Backup existing kubeconfig
echo "📦 Step 1: Backing up existing kubeconfig..."
BACKUP_FILE="${KUBE_CONFIG_PATH}.backup.$(date +%s)"
if [ -f "${KUBE_CONFIG_PATH}" ]; then
    cp "${KUBE_CONFIG_PATH}" "${BACKUP_FILE}"
    echo "✅ Backed up to: ${BACKUP_FILE}"
else
    echo "ℹ️  No existing kubeconfig found"
fi
echo ""

# 2) Test SSH connectivity
echo "🔍 Step 2: Testing SSH connectivity to new master..."
if ! ssh -o ConnectTimeout=10 -o BatchMode=yes "${SSH_USER}@${NEW_MASTER_IP}" "echo 'SSH OK'" >/dev/null 2>&1; then
    echo "❌ SSH connection failed!"
    echo "   Please ensure:"
    echo "   1. SSH key is added to the new master node"
    echo "   2. User '${SSH_USER}' exists on the master"
    echo "   3. IP address ${NEW_MASTER_IP} is correct and reachable"
    exit 1
fi
echo "✅ SSH connection successful"
echo ""

# 3) Retrieve admin.conf from new master
echo "📥 Step 3: Retrieving fresh credentials from master..."
TEMP_ADMIN_CONF="/tmp/admin.conf.new.$$"
if ! ssh "${SSH_USER}@${NEW_MASTER_IP}" "sudo cat /etc/kubernetes/admin.conf" > "${TEMP_ADMIN_CONF}" 2>/dev/null; then
    echo "❌ Failed to retrieve admin.conf from master"
    echo "   Ensure you have sudo access on the master node"
    exit 1
fi
echo "✅ Retrieved admin.conf ($(wc -l < "${TEMP_ADMIN_CONF}") lines)"
echo ""

# 4) Update server IP to public address
echo "🔧 Step 4: Updating server IP to public address..."
TEMP_UPDATED_CONF="/tmp/admin.conf.updated.$$"
sed "s|server: https://[0-9.]*:6443|server: https://${NEW_MASTER_IP}:6443|" "${TEMP_ADMIN_CONF}" > "${TEMP_UPDATED_CONF}"
echo "✅ Updated server IP"
echo ""

# 5) Add insecure-skip-tls-verify
echo "🔧 Step 5: Adding insecure-skip-tls-verify..."
sed -i.bak "/server: https:\/\/${NEW_MASTER_IP}:6443/a\\
    insecure-skip-tls-verify: true
" "${TEMP_UPDATED_CONF}"
echo "✅ Added insecure-skip-tls-verify"
echo ""

# 6) Remove certificate-authority-data (conflicts with insecure-skip-tls-verify)
echo "🔧 Step 6: Removing certificate-authority-data..."
sed -i.bak '/certificate-authority-data:/d' "${TEMP_UPDATED_CONF}"
echo "✅ Removed certificate-authority-data"
echo ""

# 7) Apply new kubeconfig
echo "📝 Step 7: Applying new kubeconfig..."
mkdir -p "$(dirname "${KUBE_CONFIG_PATH}")"
cp "${TEMP_UPDATED_CONF}" "${KUBE_CONFIG_PATH}"
chmod 600 "${KUBE_CONFIG_PATH}"
echo "✅ Applied new kubeconfig"
echo ""

# 8) Verify connectivity
echo "✅ Step 8: Verifying kubectl connectivity..."
if kubectl cluster-info >/dev/null 2>&1; then
    echo "✅ kubectl cluster-info successful"
    echo ""
    kubectl cluster-info
    echo ""
    kubectl get nodes
    echo ""
else
    echo "❌ kubectl connectivity check failed"
    echo "   Restoring backup..."
    if [ -f "${BACKUP_FILE}" ]; then
        cp "${BACKUP_FILE}" "${KUBE_CONFIG_PATH}"
        echo "   Backup restored"
    fi
    exit 1
fi

# 9) Generate base64 for GitLab CI/CD
echo ""
echo "📋 Step 9: Generating base64 kubeconfig for GitLab CI/CD..."
BASE64_OUTPUT="/tmp/kubeconfig_gitlab_base64.txt"
cat "${KUBE_CONFIG_PATH}" | base64 | tr -d '\n' > "${BASE64_OUTPUT}"
CHAR_COUNT=$(wc -c < "${BASE64_OUTPUT}" | tr -d ' ')
echo "✅ Generated base64 kubeconfig (${CHAR_COUNT} characters)"
echo "   Saved to: ${BASE64_OUTPUT}"
echo ""

# 10) Cleanup temp files
rm -f "${TEMP_ADMIN_CONF}" "${TEMP_UPDATED_CONF}" "${TEMP_UPDATED_CONF}.bak"

# Success message
echo "=========================================="
echo "✅ Kubeconfig update completed successfully!"
echo ""
echo "Next steps:"
echo "  1. Update GitLab CI/CD variable KUBECONFIG_CONTENT:"
echo "     - Go to: Settings → CI/CD → Variables"
echo "     - Edit KUBECONFIG_CONTENT"
echo "     - Paste content from: ${BASE64_OUTPUT}"
echo ""
echo "  2. Test locally:"
echo "     kubectl get nodes"
echo "     kubectl get pods -A"
echo ""
echo "Backup saved to: ${BACKUP_FILE}"
echo "=========================================="

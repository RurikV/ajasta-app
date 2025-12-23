#!/bin/bash
# Force unlock GitLab Terraform state via API
# Usage: ./force-unlock-gitlab-state.sh <lock_id> <state_name> <project_id>

set -e

LOCK_ID="${1}"
STATE_NAME="${2:-staging}"
PROJECT_ID="${3:-your-project-id}"
GITLAB_URL="${CI_API_V4_URL:-https://gitlab.com/api/v4}"

# Check for token
if [ -n "$GITLAB_TOKEN" ]; then
    TOKEN_HEADER="PRIVATE-TOKEN: ${GITLAB_TOKEN}"
elif [ -n "$CI_JOB_TOKEN" ]; then
    TOKEN_HEADER="JOB-TOKEN: ${CI_JOB_TOKEN}"
else
    echo "Error: No authentication token found"
    echo "Set GITLAB_TOKEN environment variable"
    echo "Example: export GITLAB_TOKEN=glpat-xxxxxxxxxxxxxxxxxxxx"
    exit 1
fi

echo "=== GitLab Terraform State Force Unlock ==="
echo "State: ${STATE_NAME}"
echo "Lock ID: ${LOCK_ID}"
echo "Project ID: ${PROJECT_ID}"
echo "GitLab URL: ${GITLAB_URL}"
echo ""

# Unlock via GitLab API
UNLOCK_URL="${GITLAB_URL}/projects/${PROJECT_ID}/terraform/state/${STATE_NAME}/lock"

echo "Sending DELETE request to:"
echo "  ${UNLOCK_URL}"
echo ""

RESPONSE=$(curl --silent --request DELETE \
  --header "${TOKEN_HEADER}" \
  --header "Content-Type: application/json" \
  "${UNLOCK_URL}" \
  --write-out "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | grep -v "HTTP_STATUS" || echo "")

echo "Response: $BODY"
echo "HTTP Status: $HTTP_STATUS"
echo ""

if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "204" ] || [ "$HTTP_STATUS" = "202" ]; then
    echo "✅ Success: State unlocked successfully!"
    exit 0
elif [ "$HTTP_STATUS" = "404" ]; then
    echo "ℹ️  Info: Lock not found (may have already expired)"
    exit 0
elif [ "$HTTP_STATUS" = "401" ]; then
    echo "❌ Error: Authentication failed. Check your token."
    exit 1
elif [ "$HTTP_STATUS" = "403" ]; then
    echo "❌ Error: Forbidden. You need Maintainer or Owner permissions."
    exit 1
else
    echo "⚠️  Warning: Unexpected response (HTTP $HTTP_STATUS)"
    echo "The lock may have auto-expired. Try running terraform plan again."
    exit 0
fi

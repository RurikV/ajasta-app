#!/bin/bash
# Quick unlock script for GitLab Terraform state
# This is a simplified version

set -e

TOKEN="${1:-glpat-o9enasy8qUsiKZQDBQWP}"
STATE_NAME="${2:-staging}"

# Get project ID
REMOTE_URL=$(git remote get-url origin)
PROJECT_PATH=$(echo "$REMOTE_URL" | sed 's|.*gitlab.yandexcloud.net/||' | sed 's|\.git$||' | tr -d '\n\r' | xargs)

echo "=== Quick Terraform State Unlock ==="
echo "Project: ${PROJECT_PATH}"
echo "State: ${STATE_NAME}"
echo ""

# Get project ID via API (URL-encode the path)
ENCODED_PATH=$(echo -n "$PROJECT_PATH" | jq -sRr @uri)
PROJECT_ID=$(curl --silent --header "PRIVATE-TOKEN: ${TOKEN}" \
    "https://otusteam.gitlab.yandexcloud.net/api/v4/projects/${ENCODED_PATH}" | \
    python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")

echo "Project ID: ${PROJECT_ID}"
echo ""

# Unlock the state
echo "Attempting to unlock..."
RESPONSE=$(curl --silent --write-out "\n%{http_code}" \
    --request DELETE \
    --header "PRIVATE-TOKEN: ${TOKEN}" \
    "https://otusteam.gitlab.yandexcloud.net/api/v4/projects/${PROJECT_ID}/terraform/state/${STATE_NAME}/lock")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "204" ]; then
    echo "✅ Success! State unlocked"
    echo ""
    echo "You can now run terraform commands:"
    echo "  terraform plan -out=plan.tfplan -input=false"
    echo "  terraform apply plan.tfplan"
else
    echo "⚠️  Unexpected response (HTTP ${HTTP_CODE})"
    echo "This might mean:"
    echo "  1. State wasn't locked (no problem!)"
    echo "  2. Lock already expired (no problem!)"
    echo "  3. Permission denied (check your token)"
    echo ""
    echo "Try running terraform plan anyway:"
    echo "  terraform plan -out=plan.tfplan -input=false"
fi

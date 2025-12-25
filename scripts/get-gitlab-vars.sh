#!/bin/bash
# Fetch GitLab CI/CD variables and export them as environment variables
# Usage: source ./scripts/get-gitlab-vars.sh

set -e

# Check for GitLab token and project info
if [ -z "${GITLAB_PAT}" ] && [ -z "${GITLAB_TOKEN}" ]; then
    echo "❌ ERROR: GitLab authentication not configured!"
    echo ""
    echo "Please set GITLAB_PAT environment variable:"
    echo "  export GITLAB_PAT=\"glpat-xxxxxxxxxxxxxxxxxxxx\""
    echo ""
    echo "This script will fetch these GitLab CI/CD variables:"
    echo "  - YC_CLOUD_ID"
    echo "  - YC_FOLDER_ID"
    echo "  - YC_TOKEN"
    exit 1
fi

# Determine GitLab URL and project ID
GITLAB_URL="${CI_API_V4_URL:-https://otusteam.gitlab.yandexcloud.net/api/v4}"
PROJECT_ID="${CI_PROJECT_ID:-1305}"  # Default to ajasta-app project

# Use GITLAB_PAT if set, otherwise fallback to GITLAB_TOKEN
TOKEN="${GITLAB_PAT:-${GITLAB_TOKEN}}"

echo "🔍 Fetching GitLab CI/CD variables from:"
echo "   ${GITLAB_URL}/projects/${PROJECT_ID}"
echo ""

# Function to fetch a variable value
fetch_variable() {
    local var_name="$1"
    local response

    response=$(curl --silent --request GET \
        --header "PRIVATE-TOKEN: ${TOKEN}" \
        "${GITLAB_URL}/projects/${PROJECT_ID}/variables/${var_name}" || echo "")

    if [ -n "$response" ]; then
        echo "$response" | jq -r '.value' 2>/dev/null || echo ""
    else
        echo ""
    fi
}

# Fetch Yandex Cloud variables
echo " Fetching variables..."

YC_CLOUD_ID_VALUE=$(fetch_variable "YC_CLOUD_ID")
YC_FOLDER_ID_VALUE=$(fetch_variable "YC_FOLDER_ID")
YC_TOKEN_VALUE=$(fetch_variable "YC_TOKEN")

if [ -n "$YC_CLOUD_ID_VALUE" ]; then
    export YC_CLOUD_ID="$YC_CLOUD_ID_VALUE"
    echo "  ✅ YC_CLOUD_ID fetched"
else
    echo "  ⚠️  YC_CLOUD_ID not found (using environment variable if set)"
fi

if [ -n "$YC_FOLDER_ID_VALUE" ]; then
    export YC_FOLDER_ID="$YC_FOLDER_ID_VALUE"
    echo "  ✅ YC_FOLDER_ID fetched"
else
    echo "  ⚠️  YC_FOLDER_ID not found (using environment variable if set)"
fi

if [ -n "$YC_TOKEN_VALUE" ]; then
    export YC_TOKEN="$YC_TOKEN_VALUE"
    echo "  ✅ YC_TOKEN fetched"
else
    echo "  ⚠️  YC_TOKEN not found (using environment variable if set)"
fi

echo ""
echo "✅ GitLab variables loaded!"

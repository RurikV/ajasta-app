#!/bin/bash
set -e

echo "=== Generating Terraform Outputs from Current State ==="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load GitLab CI/CD variables if needed
if [ -z "$YC_TOKEN" ]; then
  PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
  source "${PROJECT_ROOT}/scripts/get-gitlab-vars.sh"
fi

# Check if we have errored.tfstate from the pipeline
if [ -f "errored.tfstate" ]; then
  echo "✅ Found errored.tfstate from failed pipeline"

  # Try to push it to GitLab first
  export GITLAB_PAT="${GITLAB_PAT:-glpat-o9enasy8qUsiKZQDBQWP}"
  export GITLAB_USERNAME="${GITLAB_USERNAME:-Vladimir.Rurik}"
  export PROJECT_ID="1305"

  export TF_HTTP_ADDRESS="https://otusteam.gitlab.yandexcloud.net/api/v4/projects/${PROJECT_ID}/terraform/state/production"
  export TF_HTTP_LOCK_ADDRESS="${TF_HTTP_ADDRESS}/lock"
  export TF_HTTP_UNLOCK_ADDRESS="${TF_HTTP_ADDRESS}/lock"
  export TF_HTTP_USERNAME="${GITLAB_USERNAME}"
  export TF_HTTP_PASSWORD="${GITLAB_PAT}"

  echo "📤 Pushing state to GitLab..."
  if terraform state push -lock=false errored.tfstate; then
    echo "✅ State pushed successfully"
    rm -f errored.tfstate
  else
    echo "⚠️  Could not push state, using local state for outputs"
    cp errored.tfstate .terraform/terraform.tfstate
  fi
fi

# Generate outputs
echo ""
echo "📊 Generating outputs..."
terraform output -json > outputs.json

if [ -s outputs.json ]; then
  echo "✅ Outputs generated successfully: outputs.json"
  echo ""
  echo "📋 Output contents:"
  cat outputs.json | jq '.'
else
  echo "❌ Failed to generate outputs"
  exit 1
fi

echo ""
echo "🔧 You can now run:"
echo "  ../ansible-ci/scripts/generate-inventory-from-terraform.sh"

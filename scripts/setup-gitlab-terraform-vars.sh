#!/bin/bash

# GitLab Terraform Backend Configuration Setup Script
# This script helps you set up the required GitLab CI/CD variables for Terraform state management

echo "🔧 GitLab Terraform Backend Configuration Setup"
echo "=============================================="
echo ""

# Check if git command is available
if ! command -v git &> /dev/null; then
    echo "❌ Error: git command not found"
    exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --is-inside-work-tree &> /dev/null; then
    echo "❌ Error: Not in a git repository"
    exit 1
fi

# Try to get GitLab remote URL
GIT_REMOTE=$(git remote get-url origin 2>/dev/null || echo "")
echo "📋 Detected Git Remote: $GIT_REMOTE"

if [[ ! "$GIT_REMOTE" =~ gitlab ]]; then
    echo "⚠️  Warning: This doesn't appear to be a GitLab repository"
    echo "   Please ensure you're running this in a GitLab project"
fi

echo ""
echo "📝 Required GitLab CI/CD Variables"
echo "=================================="
echo ""

echo "You need to add these variables to your GitLab project:"
echo ""
echo "GitLab Project → Settings → CI/CD → Variables → Add Variable"
echo ""

# Template for variables
cat << 'EOF'
REQUIRED VARIABLES:

1. TF_HTTP_USERNAME
   Type: Variable
   Value: gitlab-ci-token
   Flags: Protected ✅, Masked ❌

2. TF_HTTP_PASSWORD
   Type: Variable
   Value: ${CI_JOB_TOKEN}
   Flags: Protected ✅, Masked ✅

3. TF_HTTP_ADDRESS
   Type: Variable
   Value: ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/production
   Flags: Protected ✅, Masked ❌

4. TF_HTTP_LOCK_ADDRESS
   Type: Variable
   Value: ${CI_HTTP_ADDRESS}/lock
   Flags: Protected ✅, Masked ❌

5. TF_HTTP_UNLOCK_ADDRESS
   Type: Variable
   Value: ${CI_HTTP_ADDRESS}/lock
   Flags: Protected ✅, Masked ❌

EOF

echo "🔍 Alternative: Auto-generated Variables"
echo "======================================="
echo ""

cat << 'EOF'
If you prefer, you can use this simplified approach:

1. TF_HTTP_USERNAME
   Type: Variable
   Value: gitlab-ci-token
   Flags: Protected ✅

2. TF_HTTP_PASSWORD
   Type: Variable
   Value: ${CI_JOB_TOKEN}
   Flags: Protected ✅, Masked ✅

Then let GitLab auto-generate the URLs during the pipeline.
EOF

echo ""
echo "⚠️  Important Notes:"
echo "=================="
echo "• The variables must be set as 'Type: Variable' (not File)"
echo "• Use ${CI_JOB_TOKEN} literally - GitLab will expand this automatically"
echo "• Protected variables only work with protected branches"
echo "• You need 'Maintainer' role or higher to set CI/CD variables"
echo ""

echo "🚀 Quick Setup Steps:"
echo "===================="
echo "1. Go to your GitLab project"
echo "2. Settings → CI/CD → Variables → Expand"
echo "3. Click 'Add variable'"
echo "4. Add the variables listed above"
echo "5. Run your pipeline again"
echo ""

echo "🧪 Verification:"
echo "================"
echo "After setting the variables, your pipeline should show:"
echo "✅ TF_HTTP_ADDRESS: [your-gitlab-url]"
echo "✅ TF_HTTP_USERNAME: gitlab-ci-token"
echo "✅ TF_HTTP_PASSWORD: [SET]"
echo ""

echo "🎯 Expected Success:"
echo "=================="
echo "Terraform initialization successful!"
echo "Terraform plan created successfully!"
echo "Terraform apply successful!"
echo ""
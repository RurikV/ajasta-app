#!/bin/bash
# Terraform wrapper with automatic state lock handling
# Usage: terraform-with-retry.sh <terraform-command> [args...]

set -e

MAX_RETRIES=3
RETRY_DELAY=30
LOCK_ERROR="Error acquiring the state lock"
LOCK_RELEASE_ERROR="Error releasing the state lock"

terraform_cmd="$@"

echo "=== Terraform with Automatic Lock Handling ==="
echo "Command: terraform $terraform_cmd"
echo "Max retries: $MAX_RETRIES"
echo "Retry delay: ${RETRY_DELAY}s"
echo ""

for attempt in $(seq 1 $MAX_RETRIES); do
    echo "Attempt $attempt of $MAX_RETRIES..."

    # Run terraform and capture output
    set +e
    output=$(terraform $terraform_cmd 2>&1)
    exit_code=$?
    set -e

    # Check if command succeeded
    if [ $exit_code -eq 0 ]; then
        echo "✅ Success: Terraform command completed successfully"
        exit 0
    fi

    # Check for lock errors
    if echo "$output" | grep -q "$LOCK_ERROR"; then
        echo "⚠️  State lock detected"

        if [ $attempt -lt $MAX_RETRIES ]; then
            echo "Waiting ${RETRY_DELAY}s for lock to be released..."
            echo "Lock info from error:"
            echo "$output" | grep -A 10 "Lock Info:" || true
            echo ""

            # Try to force unlock via API
            echo "Attempting to force unlock via GitLab API..."
            if [ -n "$CI_JOB_TOKEN" ] && [ -n "$CI_PROJECT_ID" ]; then
                state_name="${TF_STATE_NAME:-staging}"
                unlock_url="${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${state_name}/lock"

                unlock_response=$(curl --silent --request DELETE \
                    --header "JOB-TOKEN: ${CI_JOB_TOKEN}" \
                    "$unlock_url" \
                    --write-out "\nHTTP_STATUS:%{http_code}")

                unlock_status=$(echo "$unlock_response" | grep "HTTP_STATUS" | cut -d: -f2)

                if [ "$unlock_status" = "200" ] || [ "$unlock_status" = "204" ] || [ "$unlock_status" = "202" ] || [ "$unlock_status" = "404" ]; then
                    echo "✅ Force unlock successful"
                else
                    echo "⚠️  Force unlock failed (HTTP $unlock_status)"
                fi
            fi

            echo "Waiting ${RETRY_DELAY}s before retry..."
            sleep $RETRY_DELAY
            echo ""
        else
            echo "❌ Error: Max retries reached. State is still locked."
            echo ""
            echo "To manually unlock:"
            echo "  1. Go to: ${CI_SERVER_URL}/${CI_PROJECT_PATH}/-/settings/infrastructure"
            echo "  2. Find the Terraform state: ${state_name}"
            echo "  3. Click 'Force unlock'"
            echo ""
            echo "Or run locally:"
            echo "  terraform force-unlock <lock-id>"
            exit 1
        fi
    # Check for lock release error (non-critical, plan was successful)
    elif echo "$output" | grep -q "$LOCK_RELEASE_ERROR"; then
        echo "⚠️  Lock release error detected, but command may have succeeded"
        echo "Checking if plan file was created..."

        if [ -f "plan.tfplan" ]; then
            echo "✅ Plan file created successfully (lock release error is non-critical)"
            echo "The lock will auto-expire shortly. This error can be ignored."
            exit 0
        else
            echo "❌ Plan file not created. This might be a real error."
            echo "$output"
            exit 1
        fi
    else
        # Some other error
        echo "❌ Error: Terraform command failed"
        echo "$output"
        exit 1
    fi
done

echo "❌ Unexpected: Should not reach here"
exit 1

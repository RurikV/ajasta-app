#!/bin/bash
# Terraform wrapper with smart state lock handling
# This script waits for existing locks to be released instead of creating new lock attempts

set -e

MAX_WAIT_TIME=300  # Maximum wait time in seconds (5 minutes)
CHECK_INTERVAL=10  # Check every 10 seconds
LOCK_ERROR="Error acquiring the state lock"
LOCK_RELEASE_ERROR="Error releasing the state lock"

terraform_cmd="$@"

echo "=== Terraform with Smart Lock Handling ==="
echo "Command: terraform $terraform_cmd"
echo "Max wait time: ${MAX_WAIT_TIME}s"
echo "Check interval: ${CHECK_INTERVAL}s"
echo ""

# Function to extract lock ID from error message
extract_lock_id() {
    echo "$1" | grep -A 1 "Lock Info:" | grep "ID:" | awk '{print $2}' | tr -d '\r'
}

# Function to check if lock exists via API
check_lock_via_api() {
    local state_name="${TF_STATE_NAME:-staging}"

    if [ -z "$CI_JOB_TOKEN" ] || [ -z "$CI_PROJECT_ID" ]; then
        return 1  # Not in GitLab CI, can't check API
    fi

    local lock_url="${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${state_name}/lock"

    local response=$(curl --silent --request GET \
        --header "JOB-TOKEN: ${CI_JOB_TOKEN}" \
        "$lock_url" \
        --write-out "\nHTTP_STATUS:%{http_code}")

    local http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)

    # 404 means no lock (good), 200 means lock exists (bad)
    if [ "$http_status" = "404" ]; then
        return 0  # No lock
    elif [ "$http_status" = "200" ]; then
        return 1  # Lock exists
    else
        return 2  # Error checking
    fi
}

# Function to force unlock via API
force_unlock_via_api() {
    local state_name="${TF_STATE_NAME:-staging}"

    if [ -z "$CI_JOB_TOKEN" ] || [ -z "$CI_PROJECT_ID" ]; then
        echo "  Not in GitLab CI/CD, skipping API force unlock"
        return 1
    fi

    local lock_url="${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${state_name}/lock"

    echo "  Attempting force unlock via GitLab API..."

    local response=$(curl --silent --request DELETE \
        --header "JOB-TOKEN: ${CI_JOB_TOKEN}" \
        "$lock_url" \
        --write-out "\nHTTP_STATUS:%{http_code}")

    local http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)

    if [ "$http_status" = "200" ] || [ "$http_status" = "204" ] || [ "$http_status" = "202" ]; then
        echo "  ✅ Force unlock successful via API"
        return 0
    elif [ "$http_status" = "404" ]; then
        echo "  ℹ️  Lock not found (may have already expired)"
        return 0
    else
        echo "  ⚠️  Force unlock failed (HTTP $http_status)"
        return 1
    fi
}

# Main logic
elapsed_time=0
attempt_num=1

while [ $elapsed_time -lt $MAX_WAIT_TIME ]; do
    echo "Attempt $attempt_num (elapsed: ${elapsed_time}s / max: ${MAX_WAIT_TIME}s)..."

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

    # Check for lock release error (non-critical, plan was successful)
    if echo "$output" | grep -q "$LOCK_RELEASE_ERROR"; then
        echo "⚠️  Lock release error detected, checking if command succeeded..."
        if [ -f "plan.tfplan" ] || echo "$output" | grep -q "Plan:"; then
            echo "✅ Plan created successfully (lock release error is non-critical)"
            echo "The lock will auto-expire shortly. This error can be ignored."
            exit 0
        fi
    fi

    # Check for lock acquisition error
    if echo "$output" | grep -q "$LOCK_ERROR"; then
        lock_id=$(extract_lock_id "$output")

        echo "⚠️  State lock detected (ID: ${lock_id})"
        echo "Lock info:"
        echo "$output" | grep -A 10 "Lock Info:" | sed 's/^/  /' || true

        # Try to force unlock if we're in GitLab CI/CD
        if [ -n "$CI_JOB_TOKEN" ]; then
            force_unlock_via_api
            # Give it a moment to take effect
            sleep 5
        fi

        # Check if we should wait or give up
        if [ $elapsed_time -ge $MAX_WAIT_TIME ]; then
            echo ""
            echo "❌ Error: Maximum wait time reached (${MAX_WAIT_TIME}s)"
            echo ""
            echo "The state is still locked. This could mean:"
            echo "  1. Another Terraform operation is in progress"
            echo "  2. A previous operation crashed without releasing the lock"
            echo ""
            echo "To manually unlock:"
            if [ -n "$CI_SERVER_URL" ] && [ -n "$CI_PROJECT_PATH" ]; then
                echo "  1. Go to: ${CI_SERVER_URL}/${CI_PROJECT_PATH}/-/settings/infrastructure"
            else
                echo "  1. Go to your GitLab project → Infrastructure → Terraform"
            fi
            echo "  2. Find the Terraform state: ${TF_STATE_NAME:-staging}"
            echo "  3. Click 'Force unlock'"
            echo ""
            echo "Or run locally:"
            echo "  cd terraform"
            echo "  terraform force-unlock ${lock_id}"
            exit 1
        fi

        # Wait before next attempt
        echo ""
        echo "Waiting ${CHECK_INTERVAL}s before checking again..."
        echo ""
        sleep $CHECK_INTERVAL
        elapsed_time=$((elapsed_time + CHECK_INTERVAL))
        attempt_num=$((attempt_num + 1))
    else
        # Some other error
        echo "❌ Error: Terraform command failed (not a lock error)"
        echo ""
        echo "$output"
        exit 1
    fi
done

echo "❌ Unexpected: Should not reach here"
exit 1

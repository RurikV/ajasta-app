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
        # Check if plan was actually created
        if echo "$output" | grep -q "Plan:"; then
            echo "✅ Plan created successfully (lock release error is non-critical)"
            echo "The lock will auto-expire shortly. This error can be ignored."
            exit 0
        fi
        if [ -f "plan.tfplan" ]; then
            echo "✅ Plan file exists (lock release error is non-critical)"
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
        echo ""

        # Note: We do NOT attempt to force unlock via API because:
        # 1. CI job tokens often don't have permission to delete locks
        # 2. Force unlocking can be dangerous if another process is legitimately running
        # 3. Locks auto-expire after a few minutes anyway
        echo "ℹ️  Waiting for lock to be released or expire..."
        echo "   (GitLab CI job tokens cannot force unlock locks for security reasons)"

        # Check if we should give up
        if [ $elapsed_time -ge $((MAX_WAIT_TIME - CHECK_INTERVAL)) ]; then
            echo ""
            echo "❌ Error: Maximum wait time reached (${MAX_WAIT_TIME}s)"
            echo ""
            echo "The state is still locked. This could mean:"
            echo "  1. Another Terraform operation is in progress"
            echo "  2. A previous operation crashed without releasing the lock"
            echo "  3. The lock hasn't auto-expired yet"
            echo ""
            echo "To manually unlock via GitLab web UI:"
            if [ -n "$CI_SERVER_URL" ] && [ -n "$CI_PROJECT_PATH" ]; then
                echo "  1. Go to: ${CI_SERVER_URL}/${CI_PROJECT_PATH}/-/settings/infrastructure"
            else
                echo "  1. Go to your GitLab project → Infrastructure → Terraform"
            fi
            echo "  2. Find the Terraform state: ${TF_STATE_NAME:-staging}"
            echo "  3. Click 'Force unlock'"
            echo ""
            echo "Or run locally (if you have the lock ID):"
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

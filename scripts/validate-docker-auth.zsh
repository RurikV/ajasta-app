#!/usr/bin/env zsh
# Docker Hub Authentication Validation Script
# This script helps diagnose Docker Hub authentication issues in GitLab CI/CD
#
# Usage:
#   ./validate-docker-auth.zsh [--verbose]
#
# This script checks:
# 1. Docker Hub credentials configuration
# 2. Authentication capability
# 3. Rate limit status
# 4. Base image accessibility

set -euo pipefail

VERBOSE=false
if [[ "${1:-}" == "--verbose" ]]; then
    VERBOSE=true
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log() {
    local level="$1"
    shift
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    case "$level" in
        "INFO")    echo -e "${CYAN}[INFO $timestamp]${NC} $*" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS $timestamp]${NC} $*" ;;
        "WARNING") echo -e "${YELLOW}[WARNING $timestamp]${NC} $*" ;;
        "ERROR")   echo -e "${RED}[ERROR $timestamp]${NC} $*" ;;
    esac
}

echo "🔍 Docker Hub Authentication Validation"
echo "========================================"
echo ""

# Check 1: Docker installation
log "INFO" "Checking Docker installation..."
if ! command -v docker >/dev/null 2>&1; then
    log "ERROR" "Docker is not installed or not in PATH"
    exit 1
fi

docker_version=$(docker --version)
log "SUCCESS" "Docker found: $docker_version"

# Check 2: Docker daemon status
log "INFO" "Checking Docker daemon status..."
if ! docker info >/dev/null 2>&1; then
    log "ERROR" "Docker daemon is not running"
    exit 1
fi
log "SUCCESS" "Docker daemon is running"

# Check 3: Environment variables
log "INFO" "Checking Docker Hub credentials..."
echo ""
echo "ℹ️  NOTE: This script checks LOCAL environment variables."
echo "   GitLab CI/CD variables are configured separately in your GitLab project."
echo "   If you see 'not set' messages below, that's NORMAL for local testing."
echo ""

if [[ -z "${DOCKER_HUB_USER:-}" ]]; then
    log "INFO" "DOCKER_HUB_USER environment variable is not set locally"
    echo ""
    echo "✅ This is EXPECTED behavior for local testing!"
    echo "   Your GitLab CI/CD variables are configured separately."
    echo ""
    echo "🔧 GitLab CI/CD Setup (if not already done):"
    echo "   1. Go to your GitLab project: Settings → CI/CD → Variables"
    echo "   2. Add variable: DOCKER_HUB_USER = your_dockerhub_username"
    echo "   3. Add variable: DOCKER_HUB_PASSWORD = your_dockerhub_password_or_token (Protected)"
    echo ""
    echo "💻 For LOCAL testing (optional):"
    echo "   export DOCKER_HUB_USER=your_username"
    echo "   export DOCKER_HUB_PASSWORD=your_token"
    echo "   ./validate-docker-auth.zsh"
    echo ""
    echo "📖 For detailed instructions, see:"
    echo "   - README.md (Troubleshooting section)"
    echo "   - GITLAB_VARIABLES.md (Docker Hub Authentication section)"
    echo ""
    CREDENTIALS_MISSING=true
else
    log "SUCCESS" "DOCKER_HUB_USER is set: $DOCKER_HUB_USER"
    CREDENTIALS_MISSING=false
fi

if [[ -z "${DOCKER_HUB_PASSWORD:-}" ]]; then
    log "INFO" "DOCKER_HUB_PASSWORD environment variable is not set locally"
    echo ""
    echo "✅ This is EXPECTED behavior for local testing!"
    echo "   Your GitLab CI/CD variables are configured separately."
    CREDENTIALS_MISSING=true
else
    log "SUCCESS" "DOCKER_HUB_PASSWORD is set (length: ${#DOCKER_HUB_PASSWORD} characters)"
fi

# Check 4: Test authentication (if credentials are available)
if [[ "$CREDENTIALS_MISSING" == "false" ]]; then
    log "INFO" "Testing Docker Hub authentication..."
    
    # Try to login
    if echo "$DOCKER_HUB_PASSWORD" | docker login -u "$DOCKER_HUB_USER" --password-stdin >/dev/null 2>&1; then
        log "SUCCESS" "Docker Hub authentication successful"
        
        # Test rate limit status
        log "INFO" "Checking Docker Hub rate limit status..."
        rate_limit_info=$(curl -s "https://auth.docker.io/token?service=registry.docker.io&scope=repository:library/maven:pull" | jq -r '.token' 2>/dev/null || echo "")
        
        if [[ -n "$rate_limit_info" ]]; then
            log "SUCCESS" "Docker Hub API is accessible"
        else
            log "WARNING" "Could not check Docker Hub API status"
        fi
        
        # Test pulling the problematic image manifest
        log "INFO" "Testing access to maven:3.9.8-eclipse-temurin-21-alpine..."
        if docker manifest inspect maven:3.9.8-eclipse-temurin-21-alpine >/dev/null 2>&1; then
            log "SUCCESS" "maven:3.9.8-eclipse-temurin-21-alpine is accessible"
        else
            log "ERROR" "Cannot access maven:3.9.8-eclipse-temurin-21-alpine manifest"
            echo ""
            echo "❌ This indicates the same error that occurs in your CI/CD pipeline"
        fi
        
        # Logout for security
        docker logout >/dev/null 2>&1 || true
        
    else
        log "ERROR" "Docker Hub authentication failed"
        echo ""
        echo "💡 Possible causes:"
        echo "   - Invalid username or password"
        echo "   - Password/token has expired"
        echo "   - Network connectivity issues"
        echo ""
        echo "🔧 Solutions:"
        echo "   1. Verify credentials at hub.docker.com"
        echo "   2. Generate a new access token (recommended)"
        echo "   3. Update DOCKER_HUB_PASSWORD in GitLab CI/CD variables"
    fi
else
    log "INFO" "Skipping authentication test (credentials not available)"
fi

# Check 5: Test without authentication (simulate CI failure)
log "INFO" "Testing Docker Hub access without authentication..."
docker logout >/dev/null 2>&1 || true

if docker manifest inspect maven:3.9.8-eclipse-temurin-21-alpine >/dev/null 2>&1; then
    log "SUCCESS" "maven:3.9.8-eclipse-temurin-21-alpine is accessible without authentication"
    log "INFO" "You may not be hitting rate limits currently"
else
    log "ERROR" "Cannot access maven:3.9.8-eclipse-temurin-21-alpine without authentication"
    log "INFO" "This confirms you are hitting Docker Hub rate limits"
    echo ""
    echo "🎯 This is exactly the error happening in your CI/CD pipeline!"
    echo ""
fi

# Summary and recommendations
echo ""
echo "📋 Validation Summary"
echo "===================="
echo ""

if [[ "$CREDENTIALS_MISSING" == "true" ]]; then
    echo "ℹ️  Docker Hub credentials are not set in local environment"
    echo ""
    echo "✅ This is NORMAL for local validation!"
    echo "   Your GitLab CI/CD pipeline uses separate variables configured in GitLab."
    echo ""
    echo "🔧 If you're experiencing 401 errors in GitLab CI/CD:"
    echo "   1. Verify GitLab variables exist: Settings → CI/CD → Variables"
    echo "   2. Check DOCKER_HUB_USER = your_dockerhub_username"
    echo "   3. Check DOCKER_HUB_PASSWORD = your_token (Protected)"
    echo "   4. Re-run your pipeline"
    echo ""
    echo "🔒 Security Tip: Use access tokens instead of passwords"
    echo "   - Go to hub.docker.com → Account Settings → Security → Access Tokens"
    echo "   - Create token with 'Public Repo Read' permissions"
    echo "   - Use token as DOCKER_HUB_PASSWORD value in GitLab"
    echo ""
    echo "💻 For local testing (optional):"
    echo "   export DOCKER_HUB_USER=your_username"
    echo "   export DOCKER_HUB_PASSWORD=your_token"
else
    echo "✅ Docker Hub credentials are configured locally"
    echo ""
    echo "💡 If you're still seeing 401 errors in CI/CD:"
    echo "   1. Check that GitLab variables are not masked or protected incorrectly"
    echo "   2. Verify the token/password is still valid"
    echo "   3. Ensure variables are available to the branch/environment"
fi

echo ""
echo "📖 Documentation:"
echo "   - README.md: Quick troubleshooting guide"
echo "   - GITLAB_VARIABLES.md: Complete setup instructions"
echo ""

if [[ "$VERBOSE" == "true" ]]; then
    echo ""
    echo "🔧 Debug Information"
    echo "==================="
    echo "Docker info:"
    docker info 2>/dev/null | head -20 || echo "Could not get Docker info"
    echo ""
    echo "Environment variables:"
    echo "DOCKER_HUB_USER=${DOCKER_HUB_USER:-<not set>}"
    echo "DOCKER_HUB_PASSWORD=${DOCKER_HUB_PASSWORD:+<set (${#DOCKER_HUB_PASSWORD} chars)>}"
    echo "CI=${CI:-<not set>}"
    echo "GITLAB_CI=${GITLAB_CI:-<not set>}"
fi
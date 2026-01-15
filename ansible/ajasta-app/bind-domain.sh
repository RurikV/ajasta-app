#!/bin/bash
# Bind Domain to Ajasta Application
# This script binds your domain name to the Ajasta application
#
# Usage:
#   ./bind-domain.sh [domain] [email] [-v|-vv|-vvv|-vvvv|-vvvvv]
#
# Examples:
#   ./bind-domain.sh ajasta.top                    # Default email
#   ./bind-domain.sh ajasta.top admin@ajasta.top  # Custom email
#   ./bind-domain.sh ajasta.top -vv                # With verbose output

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
INVENTORY="../k8s/inventory.ini"
DOMAIN_NAME="${1:-ajasta.top}"
EMAIL="${2:-admin@ajasta.top}"
VERBOSITY=""

# Parse command line arguments
shift 2  # Shift away domain and email
for arg in "$@"; do
    case $arg in
        -v|--verbose)
            VERBOSITY="-v"
            shift
            ;;
        -vv)
            VERBOSITY="-vv"
            shift
            ;;
        -vvv)
            VERBOSITY="-vvv"
            shift
            ;;
        -vvvv)
            VERBOSITY="-vvvv"
            shift
            ;;
        -vvvvv)
            VERBOSITY="-vvvvv"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [domain] [email] [-v|-vv|-vvv|-vvvv|-vvvvv]"
            echo ""
            echo "Arguments:"
            echo "  domain              Domain name (default: ajasta.top)"
            echo "  email               Email for Let's Encrypt (default: admin@<domain>)"
            echo "  -v, -vv, -vvv       Ansible verbosity level"
            echo ""
            echo "Examples:"
            echo "  $0                              # Bind ajasta.top with default email"
            echo "  $0 ajaste.top                   # Bind ajasta.top with default email"
            echo "  $0 ajaste.top admin@test.com    # Bind with custom email"
            echo "  $0 -vv                          # Run with verbose output"
            echo ""
            echo "What this does:"
            echo "  1. Updates ingress with your domain name"
            echo "  2. Creates TLS certificate for your domain"
            echo "  3. Provides DNS configuration instructions"
            echo "  4. Creates verification script"
            echo ""
            echo "Requirements:"
            echo "  - Domain name you own and control"
            echo "  - Ability to configure DNS A record"
            echo "  - Port 80 accessible from internet (for Let's Encrypt)"
            echo ""
            exit 0
            ;;
    esac
done

# Functions
print_header() {
    echo ""
    echo -e "${BLUE}================================================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================================================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Log all output
LOG_FILE="domain-binding-$(date +%Y%m%d-%H%M%S).log"
exec > >(tee -a "$LOG_FILE")
exec 2>&1

print_header "DOMAIN BINDING FOR AJASTA APPLICATION"

echo "Domain Binding Started at: $(date)"
echo "Log file: $LOG_FILE"
echo "Domain: $DOMAIN_NAME"
echo "Email: $EMAIL"
if [ -n "$VERBOSITY" ]; then
    echo "Verbosity level: $VERBOSITY"
fi
echo ""

# Check if inventory file exists
if [ ! -f "$INVENTORY" ]; then
    print_error "Inventory file not found: $INVENTORY"
    echo "Please ensure you're running this script from the ansible/ajasta-app directory"
    exit 1
fi

# Check Ansible is installed
if ! command -v ansible-playbook &> /dev/null; then
    print_error "ansible-playbook is not installed"
    exit 1
fi

# Display information
echo ""
print_info "This script will:"
echo "  1. Update ingress with domain: $DOMAIN_NAME"
echo "  2. Configure Let's Encrypt for: $EMAIL"
echo "  3. Create TLS certificate request"
echo "  4. Provide DNS configuration instructions"
echo "  5. Create verification script"
echo ""

print_warning "IMPORTANT: Before proceeding, ensure:"
echo "  1. You own the domain: $DOMAIN_NAME"
echo "  2. You can configure DNS for this domain"
echo "  3. Port 80 is accessible from internet"
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "Domain binding cancelled"
    exit 0
fi

# Run domain binding playbook
echo ""
print_info "Binding domain $DOMAIN_NAME to Ajasta application..."
echo ""

if ansible-playbook -i "$INVENTORY" $VERBOSITY 29-bind-domain.yml \
    -e "domain_name=$DOMAIN_NAME" \
    -e "email=$EMAIL"; then
    print_success "Domain binding completed successfully!"
else
    print_error "Domain binding failed"
    echo "Check the log file for details: $LOG_FILE"
    echo ""
    print_info "Common issues:"
    echo "  - Domain not owned by you"
    echo "  - DNS not configured correctly"
    echo "  - Port 80 not accessible from internet"
    exit 1
fi

# Summary
print_header "DOMAIN BINDING COMPLETE"

echo ""
echo -e "${GREEN}Domain $DOMAIN_NAME has been bound to Ajasta application!${NC}"
echo ""
echo "Configuration Summary:"
echo "  - Domain: $DOMAIN_NAME"
echo "  - Email: $EMAIL"
echo "  - Certificate: Let's Encrypt (staging)"
echo ""
echo "Next Steps:"
echo ""
echo "1. Configure DNS A Record:"
echo "   - Log in to your DNS provider"
echo "   - Create A record: @ → <INGRESS_IP>"
echo "   - Or: $DOMAIN_NAME → <INGRESS_IP>"
echo ""
echo "2. Wait for DNS propagation (5-30 minutes)"
echo ""
echo "3. Verify DNS:"
echo "   dig $DOMAIN_NAME"
echo "   nslookup $DOMAIN_NAME"
echo ""
echo "4. Run verification script:"
echo "   ssh <master-node> 'bash /tmp/verify-$DOMAIN_NAME.sh'"
echo ""
echo "5. Monitor certificate issuance:"
echo "   kubectl get certificate -n ajasta"
echo "   kubectl describe certificate -n ajasta ajasta-tls"
echo ""
echo "6. Test HTTPS access (after certificate issued):"
echo "   curl -I https://$DOMAIN_NAME/"
echo "   Open https://$DOMAIN_NAME in browser"
echo ""
echo "Access URLs:"
echo "  - Frontend: https://$DOMAIN_NAME/"
echo "  - Backend:  https://$DOMAIN_NAME/api"
echo ""
echo "Current: Using STAGING environment"
echo "  - Certificates are INVALID (for testing only)"
echo "  - Switch to production when ready"
echo ""
echo "Log file: $LOG_FILE"
echo "Domain binding completed at: $(date)"
echo ""

exit 0

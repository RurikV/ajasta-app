#!/bin/bash
# Complete TLS Setup for ajasta.top
# This script deploys cert-manager, configures Let's Encrypt,
# and ensures your domain ajasta.top is ready for HTTPS

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}TLS Setup for ajasta.top${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if we're in the right directory
if [ ! -f "deploy-ajasta.sh" ]; then
    echo "Error: Please run from ansible/ajasta-app directory"
    exit 1
fi

echo -e "${YELLOW}This will:${NC}"
echo "1. Deploy cert-manager"
echo "2. Configure Let's Encrypt"
echo "3. Update ingress with TLS settings"
echo "4. Provide DNS configuration instructions"
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled"
    exit 0
fi

# Deploy cert-manager and Let's Encrypt
echo -e "${GREEN}Step 1/2: Deploying cert-manager and Let's Encrypt...${NC}"
./deploy-ajasta.sh 6 -vv

# Get ingress IP
echo ""
echo -e "${GREEN}Step 2/2: DNS Configuration Instructions${NC}"
echo ""

INGRESS_IP=$(ssh -o StrictHostKeyChecking=no -i /Users/rurik/.ssh/id_rsa_k8s \
  ajasta@178.154.192.52 \
  "sudo kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type==\"InternalIP\")].address}'" 2>/dev/null)

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}DNS CONFIGURATION FOR AJASTA.TOP${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "1. Log in to porkbun.com"
echo "2. Go to Domain Management → ajasta.top"
echo "3. Create A Record:"
echo "   - Name/Host: @"
echo "   - Type: A"
echo "   - Value: ${INGRESS_IP}"
echo "   - TTL: 300"
echo ""
echo "4. Save and wait for DNS propagation (5-30 min)"
echo ""
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "After DNS propagates:"
echo ""
echo "1. Verify DNS:"
echo "   dig ajasta.top"
echo ""
echo "2. Monitor certificate:"
echo "   watch kubectl get certificate -n ajasta"
echo ""
echo "3. Test HTTPS:"
echo "   curl -I https://ajasta.top/"
echo "   open https://ajasta.top"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"

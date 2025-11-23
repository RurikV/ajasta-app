#!/bin/bash

# Yandex Cloud Resource Cleanup Script
# This script helps clean up existing resources to resolve conflicts and quota issues

set -e

echo "🧹 Starting Yandex Cloud resource cleanup..."

# Configuration
RESOURCE_NAMES=(
    "ajasta-k8s-master-ip"
    "ajasta-k8s-worker1-ip"
    "ajasta-k8s-worker2-ip"
    "ajasta-k8s-worker3-ip"
    "external-ajasta-network"
    "internal-ajasta-network"
    "ajasta-external-segment"
    "ajasta-internal-segment"
    "k8s-master"
    "k8s-worker-1"
    "k8s-worker-2"
    "k8s-worker-3"
)

# Function to check if yc CLI is installed and configured
check_yc_cli() {
    if ! command -v yc &> /dev/null; then
        echo "❌ Yandex Cloud CLI (yc) is not installed"
        echo "Please install it first: https://cloud.yandex.ru/docs/cli/quickstart"
        exit 1
    fi

    if ! yc config list &> /dev/null; then
        echo "❌ Yandex Cloud CLI is not configured"
        echo "Please run: yc init"
        exit 1
    fi

    echo "✅ Yandex Cloud CLI is configured"
}

# Function to get current cloud and folder IDs
get_cloud_context() {
    CLOUD_ID=$(yc config get cloud-id)
    FOLDER_ID=$(yc config get folder-id)

    echo "📍 Current context:"
    echo "   Cloud ID: $CLOUD_ID"
    echo "   Folder ID: $FOLDER_ID"
}

# Function to list all static IP addresses
list_static_ips() {
    echo "🔍 Listing static IP addresses..."
    yc vpc address list --folder-id=$FOLDER_ID --format yaml || true
}

# Function to list all networks
list_networks() {
    echo "🔍 Listing VPC networks..."
    yc vpc network list --folder-id=$FOLDER_ID --format yaml || true
}

# Function to list all subnets
list_subnets() {
    echo "🔍 Listing VPC subnets..."
    yc vpc subnet list --folder-id=$FOLDER_ID --format yaml || true
}

# Function to list all VMs
list_vms() {
    echo "🔍 Listing VM instances..."
    yc compute instance list --folder-id=$FOLDER_ID --format yaml || true
}

# Function to delete static IP address
delete_static_ip() {
    local name=$1
    echo "🗑️  Deleting static IP: $name"

    local ip_id=$(yc vpc address list --folder-id=$FOLDER_ID --format=json | jq -r ".[] | select(.name==\"$name\") | .id" | head -1)

    if [ -n "$ip_id" ] && [ "$ip_id" != "null" ]; then
        yc vpc address delete --id=$ip_id
        echo "✅ Deleted static IP: $name (ID: $ip_id)"
    else
        echo "⚠️  Static IP not found: $name"
    fi
}

# Function to delete network
delete_network() {
    local name=$1
    echo "🗑️  Deleting network: $name"

    local net_id=$(yc vpc network list --folder-id=$FOLDER_ID --format=json | jq -r ".[] | select(.name==\"$name\") | .id" | head -1)

    if [ -n "$net_id" ] && [ "$net_id" != "null" ]; then
        echo "   Found network: $name (ID: $net_id)"

        # First delete all subnets in this network
        echo "   Deleting subnets in network $name..."
        yc vpc subnet list --folder-id=$FOLDER_ID --format=json | jq -r ".[] | select(.network_id==\"$net_id\") | .id" | while read subnet_id; do
            if [ -n "$subnet_id" ] && [ "$subnet_id" != "null" ]; then
                echo "   Deleting subnet: $subnet_id"
                yc vpc subnet delete --id=$subnet_id || true
                sleep 2  # Wait for subnet deletion
            fi
        done

        # Force delete the network (ignore non-empty errors)
        echo "   Force deleting network: $name"
        yc vpc network delete --id=$net_id || true

        # Wait a bit and check if it's really gone
        sleep 3
        local still_exists=$(yc vpc network list --folder-id=$FOLDER_ID --format=json | jq -r ".[] | select(.id==\"$net_id\") | .id" | head -1)
        if [ -n "$still_exists" ] && [ "$still_exists" != "null" ]; then
            echo "⚠️  Network still exists, may have dependent resources"
            echo "   Trying to find and delete attached resources..."

            # Find and delete any attached routes, security groups, etc.
            yc vpc route-table list --folder-id=$FOLDER_ID --format=json | jq -r ".[] | select(.network_id==\"$net_id\") | .id" | while read route_id; do
                if [ -n "$route_id" ] && [ "$route_id" != "null" ]; then
                    echo "   Deleting route table: $route_id"
                    yc vpc route-table delete --id=$route_id || true
                fi
            done

            # Try one more time
            yc vpc network delete --id=$net_id || echo "   ⚠️  Network deletion failed, manual intervention may be needed"
        else
            echo "✅ Deleted network: $name (ID: $net_id)"
        fi
    else
        echo "⚠️  Network not found: $name"
    fi
}

# Function to delete VM
delete_vm() {
    local name=$1
    echo "🗑️  Deleting VM: $name"

    local vm_id=$(yc compute instance list --folder-id=$FOLDER_ID --format=json | jq -r ".[] | select(.name==\"$name\") | .id" | head -1)

    if [ -n "$vm_id" ] && [ "$vm_id" != "null" ]; then
        yc compute instance delete --id=$vm_id
        echo "✅ Deleted VM: $name (ID: $vm_id)"
    else
        echo "⚠️  VM not found: $name"
    fi
}

# Function to cleanup all resources
cleanup_all() {
    echo "🧨 Starting cleanup of all resources..."

    # Step 1: List what we're about to delete
    echo ""
    echo "📋 Current resources to be cleaned up:"
    echo "VMs:"
    yc compute instance list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | startswith("k8s-")) | "  - \(.name) (\(.id))"' || echo "  (none found)"
    echo "Static IPs:"
    yc vpc address list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | contains("ajasta-k8s")) | "  - \(.name) (\(.id))"' || echo "  (none found)"
    echo "Networks:"
    yc vpc network list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | contains("ajasta")) | "  - \(.name) (\(.id))"' || echo "  (none found)"

    # Step 2: Delete VMs first (they depend on IPs and networks)
    echo ""
    echo "1️⃣ Deleting VM instances..."
    delete_vm "k8s-master"
    delete_vm "k8s-worker-1"
    delete_vm "k8s-worker-2"
    delete_vm "k8s-worker-3"

    # Wait a bit for VM deletion to complete
    echo "   Waiting for VM deletion to complete..."
    sleep 15

    # Step 3: Delete static IPs (they detach from VMs automatically)
    echo ""
    echo "2️⃣ Deleting static IP addresses..."
    delete_static_ip "ajasta-k8s-master-ip"
    delete_static_ip "ajasta-k8s-worker1-ip"
    delete_static_ip "ajasta-k8s-worker2-ip"
    delete_static_ip "ajasta-k8s-worker3-ip"

    # Step 4: Wait for IP detachment
    echo "   Waiting for IP detachment to complete..."
    sleep 10

    # Step 5: Delete networks and subnets (now they should be empty)
    echo ""
    echo "3️⃣ Deleting networks and subnets..."
    delete_network "external-ajasta-network"
    delete_network "internal-ajasta-network"

    # Step 6: Final verification
    echo ""
    echo "4️⃣ Final verification..."
    echo "Remaining VMs:"
    yc compute instance list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | startswith("k8s-")) | "  - \(.name)"' || echo "  (none - good!)"
    echo "Remaining Static IPs:"
    yc vpc address list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | contains("ajasta-k8s")) | "  - \(.name)"' || echo "  (none - good!)"
    echo "Remaining Networks:"
    yc vpc network list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | contains("ajasta")) | "  - \(.name)"' || echo "  (none - good!)"

    echo ""
    echo "✅ Cleanup completed!"
}

# Function to check quota usage
check_quotas() {
    echo "📊 Checking quota usage..."

    echo "   VM count: $(yc compute instance list --folder-id=$FOLDER_ID --format=json | jq '. | length')"
    echo "   Network count: $(yc vpc network list --folder-id=$FOLDER_ID --format=json | jq '. | length')"
    echo "   Static IP count: $(yc vpc address list --folder-id=$FOLDER_ID --format=json | jq '. | length')"

    echo "💡 To request quota increases, visit:"
    echo "   https://console.cloud.yandex.ru/folders/$FOLDER_ID/quotas"
}

# Interactive menu
show_menu() {
    echo ""
    echo "🛠️  Yandex Cloud Cleanup Menu:"
    echo "1) List all resources (safe)"
    echo "2) Delete specific static IPs"
    echo "3) Delete specific networks"
    echo "4) Delete specific VMs"
    echo "5) Full cleanup (ALL resources - DESTRUCTIVE!)"
    echo "6) Manual force cleanup (for stubborn resources)"
    echo "7) Check quota usage"
    echo "8) Exit"
    echo ""
    read -p "Choose an option (1-8): " choice

    case $choice in
        1)
            list_static_ips
            echo ""
            list_networks
            echo ""
            list_subnets
            echo ""
            list_vms
            ;;
        2)
            echo "Available static IPs:"
            yc vpc address list --folder-id=$FOLDER_ID --format=table 2>/dev/null || echo "No static IPs found"
            echo ""
            read -p "Enter static IP name to delete (or 'all' for all ajasta-* IPs): " ip_name
            if [ "$ip_name" = "all" ]; then
                delete_static_ip "ajasta-k8s-master-ip"
                delete_static_ip "ajasta-k8s-worker1-ip"
                delete_static_ip "ajasta-k8s-worker2-ip"
                delete_static_ip "ajasta-k8s-worker3-ip"
            else
                delete_static_ip "$ip_name"
            fi
            ;;
        3)
            echo "Available networks:"
            yc vpc network list --folder-id=$FOLDER_ID --format=table 2>/dev/null || echo "No networks found"
            echo ""
            read -p "Enter network name to delete (or 'all' for all ajasta-* networks): " net_name
            if [ "$net_name" = "all" ]; then
                delete_network "external-ajasta-network"
                delete_network "internal-ajasta-network"
            else
                delete_network "$net_name"
            fi
            ;;
        4)
            echo "Available VMs:"
            yc compute instance list --folder-id=$FOLDER_ID --format=table 2>/dev/null || echo "No VMs found"
            echo ""
            read -p "Enter VM name to delete (or 'all' for all k8s-* VMs): " vm_name
            if [ "$vm_name" = "all" ]; then
                delete_vm "k8s-master"
                delete_vm "k8s-worker-1"
                delete_vm "k8s-worker-2"
                delete_vm "k8s-worker-3"
            else
                delete_vm "$vm_name"
            fi
            ;;
        5)
            echo "⚠️  WARNING: This will delete ALL ajasta resources!"
            read -p "Type 'DELETE' to confirm: " confirm
            if [ "$confirm" = "DELETE" ]; then
                cleanup_all
            else
                echo "❌ Cancelled"
            fi
            ;;
        6)
            echo "🔨 Manual cleanup for stubborn resources..."
            echo "This will try force deletion methods."
            echo ""
            echo "Attempting force deletion of networks..."
            yc vpc network list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | contains("ajasta")) | .id' | while read net_id; do
                if [ -n "$net_id" ] && [ "$net_id" != "null" ]; then
                    echo "Force deleting network ID: $net_id"
                    yc vpc network delete --id=$net_id || true
                fi
            done
            echo "Attempting force deletion of remaining static IPs..."
            yc vpc address list --folder-id=$FOLDER_ID --format=json | jq -r '.[] | select(.name | contains("ajasta-k8s")) | .id' | while read ip_id; do
                if [ -n "$ip_id" ] && [ "$ip_id" != "null" ]; then
                    echo "Force deleting IP ID: $ip_id"
                    yc vpc address delete --id=$ip_id || true
                fi
            done
            echo "✅ Manual force cleanup attempted"
            ;;
        7)
            check_quotas
            ;;
        8)
            echo "👋 Goodbye!"
            exit 0
            ;;
        *)
            echo "❌ Invalid option"
            ;;
    esac
}

# Main execution
main() {
    check_yc_cli
    get_cloud_context

    # If arguments provided, run non-interactively
    if [ $# -gt 0 ]; then
        case $1 in
            "list")
                list_static_ips
                echo ""
                list_networks
                echo ""
                list_vms
                ;;
            "cleanup")
                cleanup_all
                ;;
            "quotas")
                check_quotas
                ;;
            *)
                echo "Usage: $0 [list|cleanup|quotas]"
                exit 1
                ;;
        esac
    else
        # Interactive mode
        while true; do
            show_menu
        done
    fi
}

# Run main function
main "$@"
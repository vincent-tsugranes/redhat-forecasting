#!/bin/bash

# Red Hat Weather Service - Restart All Services

echo "========================================="
echo "  Red Hat Weather Service - Restart"
echo "========================================="
echo ""

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Stop all services
echo "Stopping all services..."
"$SCRIPT_DIR/stop-all-services.sh"

echo ""
echo "Waiting 5 seconds before restart..."
sleep 5
echo ""

# Start all services
echo "Starting all services..."
"$SCRIPT_DIR/start-all-services.sh"

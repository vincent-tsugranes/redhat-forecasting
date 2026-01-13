#!/bin/bash

# Red Hat Weather Service - Stop All Services
# This script stops Frontend, Backend, and PostgreSQL

set -e

echo "========================================="
echo "  Red Hat Weather Service - Shutdown"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BACKEND_DIR="$SCRIPT_DIR/redhat-weather-service"
FRONTEND_DIR="$SCRIPT_DIR/redhat-weather-dashboard"

# Step 1: Stop Frontend
echo "Stopping Frontend (Vue.js)..."
if [ -f "$FRONTEND_DIR/logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat "$FRONTEND_DIR/logs/frontend.pid")
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        kill $FRONTEND_PID
        echo -e "${GREEN}✓ Frontend stopped (PID: $FRONTEND_PID)${NC}"
    else
        echo -e "${YELLOW}Frontend process not running${NC}"
    fi
    rm -f "$FRONTEND_DIR/logs/frontend.pid"
else
    # Try to find and kill any npm/vite process on port 5173
    if lsof -t -i:5173 > /dev/null 2>&1; then
        echo "Killing process on port 5173..."
        lsof -t -i:5173 | xargs kill -9
        echo -e "${GREEN}✓ Frontend stopped${NC}"
    else
        echo -e "${YELLOW}No frontend process found${NC}"
    fi
fi
echo ""

# Step 2: Stop Backend
echo "Stopping Backend Service (Quarkus)..."
if [ -f "$BACKEND_DIR/logs/backend.pid" ]; then
    BACKEND_PID=$(cat "$BACKEND_DIR/logs/backend.pid")
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        kill $BACKEND_PID
        sleep 2
        # Force kill if still running
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            kill -9 $BACKEND_PID
        fi
        echo -e "${GREEN}✓ Backend stopped (PID: $BACKEND_PID)${NC}"
    else
        echo -e "${YELLOW}Backend process not running${NC}"
    fi
    rm -f "$BACKEND_DIR/logs/backend.pid"
else
    # Try to find and kill any java process on port 8090
    if lsof -t -i:8090 > /dev/null 2>&1; then
        echo "Killing process on port 8090..."
        lsof -t -i:8090 | xargs kill -9
        echo -e "${GREEN}✓ Backend stopped${NC}"
    else
        echo -e "${YELLOW}No backend process found${NC}"
    fi
fi
echo ""

# Step 3: Stop PostgreSQL
echo "Stopping PostgreSQL Database..."
cd "$BACKEND_DIR"
if podman ps | grep -q weather-postgres; then
    # Try podman-compose first, fall back to podman compose
    if command -v podman-compose >/dev/null 2>&1; then
        podman-compose stop postgres
    else
        podman compose stop postgres
    fi
    echo -e "${GREEN}✓ PostgreSQL stopped${NC}"
else
    echo -e "${YELLOW}PostgreSQL not running${NC}"
fi
echo ""

# Optional: Remove PostgreSQL container and volumes
read -p "Do you want to remove PostgreSQL container and data? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing PostgreSQL container and volumes..."
    if command -v podman-compose >/dev/null 2>&1; then
        podman-compose down -v
    else
        podman compose down -v
    fi
    echo -e "${GREEN}✓ PostgreSQL container and data removed${NC}"
else
    echo "PostgreSQL container preserved (data retained)"
fi
echo ""

echo "========================================="
echo "  All Services Stopped"
echo "========================================="
echo ""
echo -e "${GREEN}All services have been shut down.${NC}"
echo ""
echo "To start services again, run:"
echo "  ./start-all-services.sh"
echo ""

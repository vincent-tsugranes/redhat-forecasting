#!/bin/bash

# Red Hat Weather Service - View Logs
# This script provides options to view different service logs

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BACKEND_DIR="$SCRIPT_DIR/redhat-weather-service"
FRONTEND_DIR="$SCRIPT_DIR/redhat-weather-dashboard"

echo "========================================="
echo "  Red Hat Weather Service - Logs"
echo "========================================="
echo ""
echo "Select which logs to view:"
echo ""
echo "  1) Backend (Quarkus)"
echo "  2) Frontend (Vue.js)"
echo "  3) PostgreSQL (Docker)"
echo "  4) All logs (split view)"
echo "  5) Exit"
echo ""
read -p "Enter your choice (1-5): " choice

case $choice in
    1)
        echo -e "${GREEN}Viewing Backend logs...${NC}"
        echo "Press Ctrl+C to exit"
        echo ""
        if [ -f "$BACKEND_DIR/logs/backend.log" ]; then
            tail -f "$BACKEND_DIR/logs/backend.log"
        else
            echo -e "${YELLOW}Backend log file not found${NC}"
            echo "Backend may not be running. Start it with: ./start-all-services.sh"
        fi
        ;;
    2)
        echo -e "${GREEN}Viewing Frontend logs...${NC}"
        echo "Press Ctrl+C to exit"
        echo ""
        if [ -f "$FRONTEND_DIR/logs/frontend.log" ]; then
            tail -f "$FRONTEND_DIR/logs/frontend.log"
        else
            echo -e "${YELLOW}Frontend log file not found${NC}"
            echo "Frontend may not be running. Start it with: ./start-all-services.sh"
        fi
        ;;
    3)
        echo -e "${GREEN}Viewing PostgreSQL logs...${NC}"
        echo "Press Ctrl+C to exit"
        echo ""
        if podman ps | grep -q weather-postgres; then
            podman logs -f weather-postgres
        else
            echo -e "${YELLOW}PostgreSQL container not running${NC}"
            echo "Start it with: ./start-all-services.sh"
        fi
        ;;
    4)
        echo -e "${GREEN}Viewing all logs (requires 'multitail' or multiple terminals)${NC}"
        echo ""
        if command -v multitail >/dev/null 2>&1; then
            multitail \
                -l "tail -f $BACKEND_DIR/logs/backend.log" \
                -l "tail -f $FRONTEND_DIR/logs/frontend.log" \
                -l "podman logs -f weather-postgres 2>&1"
        else
            echo -e "${YELLOW}multitail not installed. Showing backend logs only.${NC}"
            echo "Install multitail with: brew install multitail (macOS)"
            echo ""
            echo "Or open multiple terminals and run:"
            echo "  Terminal 1: tail -f $BACKEND_DIR/logs/backend.log"
            echo "  Terminal 2: tail -f $FRONTEND_DIR/logs/frontend.log"
            echo "  Terminal 3: podman logs -f weather-postgres"
            echo ""
            if [ -f "$BACKEND_DIR/logs/backend.log" ]; then
                tail -f "$BACKEND_DIR/logs/backend.log"
            fi
        fi
        ;;
    5)
        echo "Exiting..."
        exit 0
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

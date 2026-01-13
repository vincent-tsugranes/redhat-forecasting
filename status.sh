#!/bin/bash

# Red Hat Weather Service - Status Check
# This script checks the status of all services

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "  Red Hat Weather Service - Status"
echo "========================================="
echo ""

# Function to check if a port is in use
port_in_use() {
    lsof -i :"$1" >/dev/null 2>&1
}

# Check PostgreSQL
echo "PostgreSQL Database (Port 5438):"
if podman ps | grep -q weather-postgres; then
    if podman exec weather-postgres pg_isready -U weather -d weather_db > /dev/null 2>&1; then
        echo -e "  Status: ${GREEN}✓ Running and healthy${NC}"
    else
        echo -e "  Status: ${YELLOW}⚠ Running but not ready${NC}"
    fi

    # Get container stats
    container_id=$(podman ps -q -f name=weather-postgres)
    uptime=$(podman inspect -f '{{.State.StartedAt}}' $container_id)
    echo "  Started: $uptime"
else
    echo -e "  Status: ${RED}✗ Not running${NC}"
fi
echo ""

# Check Backend
echo "Backend Service (Port 8090):"
if port_in_use 8090; then
    # Try to hit health endpoint
    if curl -s http://localhost:8090/q/health > /dev/null 2>&1; then
        health_status=$(curl -s http://localhost:8090/q/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$health_status" = "UP" ]; then
            echo -e "  Status: ${GREEN}✓ Running and healthy${NC}"
        else
            echo -e "  Status: ${YELLOW}⚠ Running but unhealthy${NC}"
        fi
    else
        echo -e "  Status: ${YELLOW}⚠ Running but health check failed${NC}"
    fi

    # Check for PID file
    if [ -f "redhat-weather-service/logs/backend.pid" ]; then
        pid=$(cat redhat-weather-service/logs/backend.pid)
        echo "  PID: $pid"
    fi

    # Check API endpoints
    echo "  API: http://localhost:8090/api/weather"
    echo "  Swagger: http://localhost:8090/swagger-ui"
else
    echo -e "  Status: ${RED}✗ Not running${NC}"
fi
echo ""

# Check Frontend
echo "Frontend Dashboard (Port 5173):"
if port_in_use 5173; then
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo -e "  Status: ${GREEN}✓ Running${NC}"
    else
        echo -e "  Status: ${YELLOW}⚠ Port in use but not responding${NC}"
    fi

    # Check for PID file
    if [ -f "redhat-weather-dashboard/logs/frontend.pid" ]; then
        pid=$(cat redhat-weather-dashboard/logs/frontend.pid)
        echo "  PID: $pid"
    fi

    echo "  URL: http://localhost:5173"
else
    echo -e "  Status: ${RED}✗ Not running${NC}"
fi
echo ""

# Summary
echo "========================================="
echo "  Summary"
echo "========================================="

running_count=0
total_count=3

if podman ps | grep -q weather-postgres; then
    running_count=$((running_count + 1))
fi

if port_in_use 8090; then
    running_count=$((running_count + 1))
fi

if port_in_use 5173; then
    running_count=$((running_count + 1))
fi

echo "Services running: $running_count/$total_count"
echo ""

if [ $running_count -eq $total_count ]; then
    echo -e "${GREEN}✓ All services are running!${NC}"
elif [ $running_count -eq 0 ]; then
    echo -e "${RED}✗ No services are running${NC}"
    echo "Start services with: ./start-all-services.sh"
else
    echo -e "${YELLOW}⚠ Some services are not running${NC}"
    echo "Try restarting with: ./restart-all-services.sh"
fi
echo ""

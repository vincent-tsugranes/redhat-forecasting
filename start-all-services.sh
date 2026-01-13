#!/bin/bash

# Red Hat Weather Service - Start All Services
# This script starts PostgreSQL, Backend (Quarkus), and Frontend (Vue.js)

set -e

echo "========================================="
echo "  Red Hat Weather Service - Startup"
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

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is in use
port_in_use() {
    lsof -i :"$1" >/dev/null 2>&1
}

# Check prerequisites
echo "Checking prerequisites..."
if ! command_exists podman; then
    echo -e "${RED}Error: Podman is not installed${NC}"
    echo "Install with: brew install podman (macOS) or visit https://podman.io/getting-started/installation"
    exit 1
fi

if ! command_exists podman-compose; then
    echo -e "${YELLOW}Warning: podman-compose is not installed${NC}"
    echo "Install with: pip3 install podman-compose"
    echo "Attempting to use 'podman compose' instead..."
fi

if ! command_exists mvn; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    exit 1
fi

if ! command_exists npm; then
    echo -e "${RED}Error: npm is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites found${NC}"
echo ""

# Step 1: Start PostgreSQL
echo "========================================="
echo "Step 1: Starting PostgreSQL Database"
echo "========================================="
cd "$BACKEND_DIR"

if podman ps | grep -q weather-postgres; then
    echo -e "${YELLOW}PostgreSQL is already running${NC}"
else
    echo "Starting PostgreSQL container..."

    # Try podman-compose first, fall back to podman compose
    if command_exists podman-compose; then
        podman-compose up -d postgres
    else
        podman compose up -d postgres
    fi

    echo "Waiting for PostgreSQL to be ready..."
    sleep 5

    # Wait for PostgreSQL to be healthy
    max_attempts=30
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if podman exec weather-postgres pg_isready -U weather -d weather_db > /dev/null 2>&1; then
            echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
            break
        fi
        attempt=$((attempt + 1))
        echo "Waiting for PostgreSQL... ($attempt/$max_attempts)"
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        echo -e "${RED}Error: PostgreSQL failed to start${NC}"
        exit 1
    fi
fi
echo ""

# Step 2: Start Backend Service
echo "========================================="
echo "Step 2: Starting Backend Service (Quarkus)"
echo "========================================="

if port_in_use 8090; then
    echo -e "${YELLOW}Warning: Port 8090 is already in use${NC}"
    echo "Backend service may already be running"
else
    echo "Starting Quarkus backend service..."
    cd "$BACKEND_DIR"

    # Create logs directory
    mkdir -p logs

    # Start Quarkus in dev mode in the background
    echo "Running: mvn quarkus:dev"
    echo "Logs will be written to: $BACKEND_DIR/logs/backend.log"
    nohup mvn quarkus:dev > logs/backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > logs/backend.pid

    echo "Waiting for backend to start..."
    max_attempts=60
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:8090/q/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Backend service is ready${NC}"
            break
        fi
        attempt=$((attempt + 1))
        echo "Waiting for backend... ($attempt/$max_attempts)"
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        echo -e "${RED}Error: Backend service failed to start${NC}"
        echo "Check logs at: $BACKEND_DIR/logs/backend.log"
        exit 1
    fi
fi
echo ""

# Step 3: Start Frontend
echo "========================================="
echo "Step 3: Starting Frontend (Vue.js)"
echo "========================================="

if port_in_use 5173; then
    echo -e "${YELLOW}Warning: Port 5173 is already in use${NC}"
    echo "Frontend may already be running"
else
    cd "$FRONTEND_DIR"

    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        npm install
    fi

    # Create logs directory
    mkdir -p logs

    # Start Vue dev server in the background
    echo "Starting Vue.js frontend..."
    echo "Logs will be written to: $FRONTEND_DIR/logs/frontend.log"
    nohup npm run dev > logs/frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > logs/frontend.pid

    echo "Waiting for frontend to start..."
    sleep 5

    max_attempts=30
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Frontend is ready${NC}"
            break
        fi
        attempt=$((attempt + 1))
        echo "Waiting for frontend... ($attempt/$max_attempts)"
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        echo -e "${YELLOW}Warning: Frontend may not have started properly${NC}"
        echo "Check logs at: $FRONTEND_DIR/logs/frontend.log"
    fi
fi
echo ""

# Summary
echo "========================================="
echo "  All Services Started Successfully!"
echo "========================================="
echo ""
echo -e "${GREEN}Services are now running:${NC}"
echo ""
echo "  📊 Frontend Dashboard:"
echo "     http://localhost:5173"
echo ""
echo "  🔧 Backend API:"
echo "     http://localhost:8090/api/weather"
echo ""
echo "  📖 API Documentation (Swagger):"
echo "     http://localhost:8090/swagger-ui"
echo ""
echo "  ❤️  Health Check:"
echo "     http://localhost:8090/q/health"
echo ""
echo "  🗄️  PostgreSQL Database:"
echo "     localhost:5438 (weather_db)"
echo ""
echo -e "${YELLOW}To view logs:${NC}"
echo "  Backend:  tail -f $BACKEND_DIR/logs/backend.log"
echo "  Frontend: tail -f $FRONTEND_DIR/logs/frontend.log"
echo ""
echo -e "${YELLOW}To stop all services:${NC}"
echo "  ./stop-all-services.sh"
echo ""
echo "========================================="

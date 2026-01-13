# Red Hat Weather Service

A comprehensive weather data aggregation platform that collects and consolidates weather forecasts from multiple sources into a unified database with interactive visualization.

## Features

- **Multi-Source Data Collection**: Aggregates data from NOAA, Aviation Weather Center, National Hurricane Center, and OpenWeatherMap
- **Automated Updates**: Schedulers automatically fetch fresh data every 15-30 minutes
- **RESTful API**: Full CRUD operations for all weather data
- **Interactive Dashboard**: Vue.js frontend with maps and charts
- **Database**: PostgreSQL with optimized JSONB storage and indexing

## Architecture

```
redhat-forecasting/
├── redhat-weather-service/      # Backend (Java/Quarkus)
│   ├── src/main/java/          # Java source code
│   ├── src/main/resources/     # Configuration & migrations
│   ├── pom.xml                 # Maven dependencies
│   └── docker-compose.yml      # PostgreSQL setup
│
├── redhat-weather-dashboard/    # Frontend (Vue 3/TypeScript)
│   ├── src/                    # Vue components & services
│   ├── package.json            # npm dependencies
│   └── vite.config.ts          # Build configuration
│
└── *.sh                        # Convenience scripts
```

## Quick Start

### Prerequisites

- **Java 21** - [Download](https://adoptium.net/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 20+** - [Download](https://nodejs.org/)
- **Podman** - [Download](https://podman.io/getting-started/installation)
  - macOS: `brew install podman`
  - Linux: See [installation guide](https://podman.io/getting-started/installation)
  - **Important**: On macOS, initialize Podman after installation:
    ```bash
    podman machine init
    podman machine start
    ```
- **podman-compose** (optional, will fall back to `podman compose`)
  - Install: `pip3 install podman-compose`

For detailed Podman setup instructions, see [PODMAN_SETUP.md](PODMAN_SETUP.md)

### Installation & Startup

```bash
# Clone the repository (if not already done)
cd /Users/vtsugran/Code/redhat-forecasting

# Start all services (PostgreSQL, Backend, Frontend)
./start-all-services.sh
```

The script will:
1. Start PostgreSQL in Docker
2. Run database migrations
3. Start the Quarkus backend on port 8090
4. Start the Vue.js frontend on port 5173

### Access the Application

Once started, access these URLs:

- **Frontend Dashboard**: http://localhost:5173
- **Backend API**: http://localhost:8090/api/weather
- **API Documentation**: http://localhost:8090/swagger-ui
- **Health Check**: http://localhost:8090/q/health

## Management Scripts

### Start All Services
```bash
./start-all-services.sh
```
Starts PostgreSQL, backend, and frontend in the correct order.

### Stop All Services
```bash
./stop-all-services.sh
```
Gracefully stops all running services.

### Restart All Services
```bash
./restart-all-services.sh
```
Stops and restarts all services.

### Check Status
```bash
./status.sh
```
Shows the current status of all services.

### View Logs
```bash
./view-logs.sh
```
Interactive menu to view logs from different services.

## Data Sources

### NOAA/Weather.gov (Primary)
- **Update Frequency**: Every 30 minutes
- **Coverage**: United States
- **Data**: Public weather forecasts
- **API**: Free, no authentication required

### Aviation Weather Center
- **Update Frequency**: Every 15 minutes
- **Coverage**: Global airports
- **Data**: METAR (observations) and TAF (forecasts)
- **API**: Free, no authentication required

### National Hurricane Center
- **Update Frequency**: Every hour (hurricane season) / Every 6 hours (off-season)
- **Coverage**: Atlantic and Pacific basins
- **Data**: Tropical storms and hurricane tracking
- **API**: Free, no authentication required

### OpenWeatherMap (Secondary)
- **Update Frequency**: Every 2 hours (when enabled)
- **Coverage**: Global
- **Data**: Weather forecasts
- **API**: Requires free API key

#### To enable OpenWeatherMap:
1. Get a free API key at https://openweathermap.org/api
2. Set environment variable:
```bash
export OPENWEATHER_API_KEY=your_key_here
```
3. Enable in configuration:
```properties
# In redhat-weather-service/src/main/resources/application.properties
weather.scheduler.openweather.enabled=true
```

## API Endpoints

### Locations
- `GET /api/weather/locations` - List all locations
- `GET /api/weather/locations/{id}` - Get location by ID
- `GET /api/weather/locations/airports` - List all airports
- `POST /api/weather/locations` - Create new location

### Weather Forecasts
- `GET /api/weather/forecasts/location/{id}` - Get forecasts for a location
- `GET /api/weather/forecasts/coordinates?lat={lat}&lon={lon}` - Get forecasts by coordinates
- `GET /api/weather/forecasts/current?lat={lat}&lon={lon}` - Get current forecast

### Airport Weather
- `GET /api/weather/airports/{code}` - Get airport weather by ICAO code
- `GET /api/weather/airports/{code}/metar` - Get latest METAR
- `GET /api/weather/airports/{code}/taf` - Get latest TAF
- `POST /api/weather/airports/{code}/refresh` - Manually refresh airport data

### Hurricanes
- `GET /api/weather/hurricanes/active` - Get active tropical systems
- `GET /api/weather/hurricanes/{stormId}` - Get storm details
- `GET /api/weather/hurricanes/{stormId}/track` - Get storm track
- `POST /api/weather/hurricanes/refresh` - Manually refresh hurricane data

## Database

### Schema
- **locations** - Geographic locations (cities, airports)
- **weather_forecasts** - Public weather forecast data
- **airport_weather** - METAR/TAF aviation weather
- **hurricanes** - Tropical storm and hurricane data

### Sample Data
The application comes pre-loaded with 16 locations:
- 8 major US cities (NYC, LA, Chicago, Miami, Denver, Seattle, Boston, Atlanta)
- 8 major airports (JFK, LAX, ORD, MIA, DEN, SEA, BOS, ATL)

### Database Access
```bash
# Option 1: Connect via the container
podman exec -it weather-postgres psql -U weather -d weather_db

# Option 2: Connect from host (using external port 5438)
psql -h localhost -p 5438 -U weather -d weather_db

# Example queries
SELECT * FROM locations;
SELECT COUNT(*) FROM weather_forecasts;
SELECT * FROM airport_weather WHERE airport_code = 'KJFK' LIMIT 5;
SELECT * FROM hurricanes WHERE is_active = true;
```

## Development

### Backend Development
```bash
cd redhat-weather-service

# Run in dev mode (with live reload)
./mvnw quarkus:dev

# Build
./mvnw clean package

# Run tests
./mvnw test
```

### Frontend Development
```bash
cd redhat-weather-dashboard

# Install dependencies
npm install

# Run dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Configuration

### Backend Configuration
Edit `redhat-weather-service/src/main/resources/application.properties`:

```properties
# Database
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5438/weather_db
quarkus.datasource.username=weather
quarkus.datasource.password=weather

# Schedulers
weather.scheduler.noaa.enabled=true
weather.scheduler.aviation.enabled=true
weather.scheduler.hurricane.enabled=true
weather.scheduler.openweather.enabled=false

# Data retention (days)
weather.data.retention.days=7
weather.data.cleanup.days=30
```

### Frontend Configuration
The frontend automatically proxies API requests to the backend at `http://localhost:8090`.

## Troubleshooting

### Services won't start
```bash
# Check what's using the ports
lsof -i :5438  # PostgreSQL (external port)
lsof -i :8090  # Backend
lsof -i :5173  # Frontend

# Check service status
./status.sh

# View logs
./view-logs.sh
```

### Database connection issues
```bash
# Check PostgreSQL is running
podman ps | grep weather-postgres

# Check PostgreSQL logs
podman logs weather-postgres

# Restart PostgreSQL
cd redhat-weather-service
podman-compose restart postgres
# OR
podman compose restart postgres
```

### Backend compilation errors
```bash
cd redhat-weather-service

# Clean and rebuild
./mvnw clean compile

# Check Java version
java -version  # Should be 21
```

### Frontend won't start
```bash
cd redhat-weather-dashboard

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Check Node version
node --version  # Should be 20+
```

## Deployment

### Podman Deployment (Full Stack)
```bash
cd redhat-weather-service

# Using podman-compose
podman-compose up --build

# OR using podman compose
podman compose up --build
```

This will start all services in containers.

### Production Deployment
1. Build backend: `cd redhat-weather-service && ./mvnw package`
2. Build frontend: `cd redhat-weather-dashboard && npm run build`
3. Deploy backend JAR to your server
4. Serve frontend `dist/` folder with nginx or similar
5. Configure environment variables for production database

## Monitoring

### Health Checks
- Backend: http://localhost:8090/q/health
- Metrics: http://localhost:8090/q/metrics

### Logs
- Backend: `redhat-weather-service/logs/backend.log`
- Frontend: `redhat-weather-dashboard/logs/frontend.log`
- PostgreSQL: `podman logs weather-postgres`

## License

Copyright © 2024 Red Hat. All rights reserved.

## Support

For issues or questions:
1. Check the logs: `./view-logs.sh`
2. Check service status: `./status.sh`
3. Review API documentation: http://localhost:8090/swagger-ui

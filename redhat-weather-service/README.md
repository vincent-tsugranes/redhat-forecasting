# Red Hat Weather Service

A weather data aggregation microservice built with Quarkus that pulls data from multiple sources:
- NOAA/Weather.gov (public weather forecasts)
- Aviation Weather Center (airport METAR/TAF data)
- National Hurricane Center (hurricane tracking)
- OpenWeatherMap (secondary weather source)

## Prerequisites

- Java 21
- Maven 3.9+
- Podman (for local development)
  - macOS: `brew install podman`
  - Linux: See https://podman.io/getting-started/installation
- podman-compose (optional)
  - Install: `pip3 install podman-compose`

## Quick Start

### Option 1: Run with Podman Compose (Recommended)

```bash
# Start PostgreSQL database and backend service
podman-compose up --build
# OR
podman compose up --build
```

The service will be available at:
- API: http://localhost:8090
- Swagger UI: http://localhost:8090/swagger-ui
- Health Check: http://localhost:8090/q/health

### Option 2: Run with Quarkus Dev Mode

```bash
# Start PostgreSQL database
podman-compose up postgres
# OR
podman compose up postgres

# Run Quarkus in dev mode
./mvnw quarkus:dev
```

Dev mode features:
- Live reload on code changes
- Dev UI at http://localhost:8090/q/dev
- Swagger UI at http://localhost:8090/swagger-ui

## Database

The service uses PostgreSQL 16 (accessible on port **5438** to avoid conflicts) with the following schema:
- `locations` - Geographic locations for weather monitoring
- `weather_forecasts` - Public weather forecasts (NOAA, OpenWeatherMap)
- `airport_weather` - Airport METAR/TAF data
- `hurricanes` - Hurricane and tropical storm data

### Database Migrations

Flyway migrations run automatically on startup. All migrations are in `src/main/resources/db/migration/`.

### Sample Data

The V1 migration includes sample locations:
- 8 major US cities (New York, LA, Chicago, Miami, Denver, Seattle, Boston, Atlanta)
- 8 major airports (JFK, LAX, ORD, MIA, DEN, SEA, BOS, ATL)

## API Endpoints

### Locations
- `GET /api/weather/locations` - Get all locations
- `GET /api/weather/locations/{id}` - Get location by ID
- `GET /api/weather/locations/type/{type}` - Get locations by type (city, airport, region)
- `GET /api/weather/locations/airports` - Get all airports
- `GET /api/weather/locations/airport/{code}` - Get location by airport code
- `GET /api/weather/locations/search?name={name}` - Search locations by name
- `POST /api/weather/locations` - Create new location
- `PUT /api/weather/locations/{id}` - Update location
- `DELETE /api/weather/locations/{id}` - Delete location

### Weather Forecasts (Coming Soon)
- `GET /api/weather/forecasts/location/{id}` - Get forecasts for location
- `GET /api/weather/forecasts/coordinates?lat={lat}&lon={lon}&from={ISO8601}&to={ISO8601}` - Get forecasts by coordinates
- `GET /api/weather/forecasts/current?lat={lat}&lon={lon}` - Get current forecast

### Airport Weather (Coming Soon)
- `GET /api/weather/airports` - List all airports with weather
- `GET /api/weather/airports/{code}` - Get airport weather by code
- `GET /api/weather/airports/{code}/metar` - Get latest METAR
- `GET /api/weather/airports/{code}/taf` - Get latest TAF

### Hurricanes (Coming Soon)
- `GET /api/weather/hurricanes/active` - Get active hurricanes
- `GET /api/weather/hurricanes/{stormId}` - Get hurricane details
- `GET /api/weather/hurricanes/{stormId}/track` - Get hurricane track

## Configuration

Key configuration properties in `application.properties`:

### Database
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5438/weather_db
quarkus.datasource.username=weather
quarkus.datasource.password=weather
```

### Schedulers
```properties
weather.scheduler.noaa.enabled=true         # NOAA weather (every 30 min)
weather.scheduler.aviation.enabled=true     # Aviation weather (every 15 min)
weather.scheduler.hurricane.enabled=true    # Hurricanes (every hour)
weather.scheduler.openweather.enabled=false # OpenWeatherMap (disabled by default)
```

### OpenWeatherMap API Key (Optional)
```bash
export OPENWEATHER_API_KEY=your_api_key_here
```

Get a free API key at: https://openweathermap.org/api

## Testing the Service

### Check Health
```bash
curl http://localhost:8090/q/health
```

### Get All Locations
```bash
curl http://localhost:8090/api/weather/locations
```

### Get Airports Only
```bash
curl http://localhost:8090/api/weather/locations/airports
```

### Search Locations
```bash
curl "http://localhost:8090/api/weather/locations/search?name=New%20York"
```

### Create a Location
```bash
curl -X POST http://localhost:8090/api/weather/locations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "San Francisco",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "locationType": "city",
    "state": "California",
    "country": "USA"
  }'
```

## Project Structure

```
src/main/java/com/redhat/weather/
├── domain/
│   ├── entity/        # JPA entities
│   └── repository/    # Panache repositories
├── service/           # Business logic
├── resource/          # REST endpoints
├── client/            # External API clients (NOAA, Aviation, NHC, OpenWeatherMap)
├── scheduler/         # Scheduled jobs for data fetching
├── dto/               # Data transfer objects
└── mapper/            # Entity-DTO mappers

src/main/resources/
├── application.properties
└── db/migration/      # Flyway SQL migrations
```

## Development

### Building
```bash
./mvnw clean package
```

### Running Tests
```bash
./mvnw test
```

### Building Container Image
```bash
podman build -t redhat-weather-service:latest .
```

## Monitoring

- **Health Check**: http://localhost:8090/q/health
- **Metrics**: http://localhost:8090/q/metrics
- **OpenAPI Spec**: http://localhost:8090/openapi
- **Swagger UI**: http://localhost:8090/swagger-ui

## Next Steps

1. Implement REST clients for external weather APIs
2. Create service layer for weather data fetching
3. Set up schedulers for automated data collection
4. Build frontend dashboard with Vue.js
5. Add authentication and rate limiting
6. Deploy to production environment

## License

Copyright © 2024 Red Hat. All rights reserved.

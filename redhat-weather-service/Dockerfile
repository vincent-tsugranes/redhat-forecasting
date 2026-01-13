# Multi-stage Dockerfile for Quarkus application

# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 quarkus && \
    adduser -u 1001 -G quarkus -s /bin/sh -D quarkus

# Copy the built artifact from build stage
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/*.jar /app/
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/app/ /app/app/
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/quarkus/ /app/quarkus/

# Switch to non-root user
USER quarkus

# Expose port
EXPOSE 8090

# Set environment variables
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8090/q/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/quarkus-run.jar"]

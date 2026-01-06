# Multi-stage build for Metrics Reporter (build common-core + metrics-reporter)
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy parent POM + module POMs first (dependency download layer)
COPY pom.xml pom.xml
COPY common-core/pom.xml common-core/pom.xml
COPY metrics-reporter/pom.xml metrics-reporter/pom.xml

# Install parent POM into local repo so Maven can resolve it later
RUN mvn -f pom.xml -N install -DskipTests

# Pre-fetch deps for common-core
RUN mvn -f common-core/pom.xml dependency:go-offline -B

# Copy shared DTO sources and build/install it locally
COPY common-core/src common-core/src
RUN mvn -f common-core/pom.xml clean install -DskipTests

# Copy metrics-reporter sources and fetch deps (common-core now local)
COPY metrics-reporter/src metrics-reporter/src
RUN mvn -f metrics-reporter/pom.xml dependency:go-offline -B

# Build metrics-reporter
RUN mvn -f metrics-reporter/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN addgroup -S user \
 && adduser -S user -G user
USER user

# Copy the built JAR from build stage
COPY --from=build /app/metrics-reporter/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Multi-stage build for Metrics Reporter (build common-dto + metrics-reporter)
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy poms first (dependency download layer)
COPY common-dto/pom.xml common-dto/pom.xml
COPY metrics-reporter/pom.xml metrics-reporter/pom.xml

# Pre-fetch deps for common-dto
RUN mvn -f common-dto/pom.xml dependency:go-offline -B

# Copy shared DTO sources and build/install it locally
COPY common-dto/src common-dto/src
RUN mvn -f common-dto/pom.xml clean install -DskipTests

# Copy metrics-reporter sources and fetch deps (common-dto now local)
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

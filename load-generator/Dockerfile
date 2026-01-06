# Multi-stage build for Load Generator (build common-core + load-generator)
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy parent POM + module POMs first (dependency download layer)
COPY pom.xml pom.xml
COPY common-core/pom.xml common-core/pom.xml
COPY load-generator/pom.xml load-generator/pom.xml

# Install parent POM into local repo so Maven can resolve it later
RUN mvn -f pom.xml -N install -DskipTests

# Pre-fetch deps for common-core
RUN mvn -f common-core/pom.xml dependency:go-offline -B

# Copy shared DTO sources and build/install it locally
COPY common-core/src common-core/src
RUN mvn -f common-core/pom.xml clean install -DskipTests

# Copy load-generator sources and fetch deps (now common-core is in local repo)
COPY load-generator/src load-generator/src
RUN mvn -f load-generator/pom.xml dependency:go-offline -B

# Build load-generator
RUN mvn -f load-generator/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN mkdir -p /app/data \
 && addgroup -S user \
 && adduser -S user -G user \
 && chown -R user:user /app/data
USER user

EXPOSE 8081

# Copy the built JAR from build stage
COPY --from=build /app/load-generator/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

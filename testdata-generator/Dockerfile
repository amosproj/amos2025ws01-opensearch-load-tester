# Multi-stage build for Test Data Generator
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy parent POM + module POM
COPY pom.xml pom.xml
COPY testdata-generator/pom.xml testdata-generator/pom.xml

# Install parent POM into local repo so Maven can resolve it later
RUN mvn -f pom.xml -N install -DskipTests

# Pre-fetch deps for testdata-generator
RUN mvn -f testdata-generator/pom.xml dependency:go-offline -B

# Copy source code and build
COPY testdata-generator/src testdata-generator/src
RUN mvn -f testdata-generator/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create dir for persistent test data mode
# And run as non-root user
RUN mkdir -p /app/data \
 && addgroup -S user \
 && adduser -S user -G user \
 && chown -R user:user /app/data
USER user

# Copy the built JAR from build stage
COPY --from=build /app/testdata-generator/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

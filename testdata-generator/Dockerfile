# Multi-stage build for Test Data Generator
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

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
COPY --from=build /app/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]


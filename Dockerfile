# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy all project files including mvnw and .mvn folder
COPY . .

# Ensure mvnw is executable
RUN chmod +x mvnw

# Build the project (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jdk AS runtime
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

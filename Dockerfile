# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy the entire workspace into the container
COPY . .

# Step into the inner directory where pom.xml actually lives and build
WORKDIR /app/civicpulse-ai
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime environment
FROM openjdk-21
WORKDIR /app

# Copy the built jar dynamically without needing to know the exact snapshot name
COPY --from=build /app/civicpulse-ai/target/*.jar app.jar

# Spring Boot defaults to 8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
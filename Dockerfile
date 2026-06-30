# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy everything from the root directory into the container
COPY . .

# Move into the inner project folder where pom.xml actually lives
WORKDIR /app/civicpulse-ai

# Ensure the maven wrapper has execution permissions and build
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime environment using a supported Java 21 image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR from the inner target folder dynamically
COPY --from=build /app/civicpulse-ai/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
# -----------------------------
# Stage 1: Build the Spring Boot jar
# -----------------------------
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven configuration and source code
COPY pom.xml .
COPY src ./src

# Build the jar, skip tests for faster build
RUN mvn clean package -DskipTests

# -----------------------------
# Stage 2: Run the Spring Boot app
# -----------------------------
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render assigns PORT automatically)
EXPOSE 8080

# Start the app using the Render-provided PORT
CMD ["java", "-jar", "app.jar", "--server.port=8080"]

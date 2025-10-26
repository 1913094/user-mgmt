# Use OpenJDK 17
FROM openjdk:17-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy the built jar file into the container
COPY target/*.jar app.jar

# Expose port for Render
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

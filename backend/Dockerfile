# Use OpenJDK 17 JDK for the build stage
FROM openjdk:17-jdk-slim AS build

# Set the working directory $(CWD) inside the container
WORKDIR /app

# Copy all files from the local directory into the container
COPY . .

# Grant execute permissions to the Gradle wrapper
RUN chmod +x ./gradlew

# Build the application, excluding tests for a faster build
RUN ./gradlew clean bootJar -x test

# Use OpenJDK 17 slim JRE for the final runtime image
FROM openjdk:17-jdk-slim

# Set working directory for runtime
WORKDIR /app

# Copy only the built boot JAR from the build stage to the runtime image
COPY --from=build /app/build/libs/evalx-backend-1.0.0.jar app.jar

# Expose port (Render uses the PORT environment variable to route traffic)
EXPOSE 8080

# Environment variables with sensible defaults
# These are overridden by Render's environment variable settings
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=postgres

# Run the Spring Boot application
# server.port will pick up the PORT variable automatically from application.yml
ENTRYPOINT ["java", "-jar", "app.jar"]

# ---------- Build Stage ----------
# Use Eclipse Temurin JDK 17 for building
FROM eclipse-temurin:17-jdk AS build

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Make Gradle wrapper executable
RUN chmod +x ./gradlew

# Build Spring Boot JAR (skip tests for faster build)
RUN ./gradlew clean bootJar -x test


# ---------- Runtime Stage ----------
# Use lightweight JRE image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=postgres

# Run Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
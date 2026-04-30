
# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
# Copy maven executable and pom.xml to cache dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Download dependencies - this layer is cached unless pom.xml changes
RUN ./mvnw dependency:go-offline
# Copy source and build the app
COPY src ./src
RUN ./mvnw clean package -DskipTests


# --- Stage 2: Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
git p
# 1. Security: Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 2. Optimization: Copy only the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# 3. Memory Tuning: Configure JVM for container environments
# MaxRAMPercentage allows the JVM to scale with the Docker container's memory limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2 -Djava.security.egd=file:/dev/./urandom"

# 4. Observability: Expose the app port and Actuator port
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
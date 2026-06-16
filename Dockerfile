
# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
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
#Upgrade: Switched from Alpine (musl) to Ubuntu-slim (glibc) on ec2
# to optimize concurrent epoll/NIO native memory allocation mechanics.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
# 1. Security: Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 2. Optimization: Copy only the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# 3. Memory Tuning: Configure JVM for container environments
# Memory, Thread Stack, and G1GC Kernel Tuning Parameters
# Hardcoded while deploying to match  4.5 GB Heap / 2.0 GB Off-Heap allocation blueprint for an 8 GB EC2 Nodes
ENV JAVA_OPTS="-Xms1500m -Xmx1500m \
               -Xss512k \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=20 \
               -XX:InitiatingHeapOccupancyPercent=45 \
               -XX:G1ReservePercent=15 \
               -XX:ActiveProcessorCount=2 \
               -Xlog:gc* \
               -Djava.security.egd=file:/dev/./urandom"

# 4. Observability: Expose the app port and Actuator port
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
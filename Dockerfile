
# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]




## Stage 1: Build
#FROM maven:3.9.6-eclipse-temurin-21 AS builder
#WORKDIR /app
#COPY pom.xml .
## This downloads dependencies to the hidden .m2 folder
#RUN mvn dependency:go-offline
#
#COPY src ./src
## 1. Build the thin JAR
## 2. Use a maven command to copy all dependencies into a folder we can see
#RUN mvn clean package -DskipTests && \
#    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
#
## Stage 2: Run
#FROM eclipse-temurin:21-jre-alpine
#WORKDIR /app
#
## Copy the thin JAR
#COPY --from=builder /app/target/*.jar app.jar
## Copy the folder containing all your library JARs (Kafka, Redis, etc.)
#COPY --from=builder /app/target/dependency ./lib
#
#EXPOSE 8080
#
## Use the -cp (Classpath) flag to tell Java:
## "Look in app.jar AND look in the lib folder for everything else"
#ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "com.yourpackage.MainClassName"]
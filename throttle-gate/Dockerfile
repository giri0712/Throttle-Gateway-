# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage — 17-jre is the current LTS runtime line
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/throttle-gate-1.0.0-SNAPSHOT.jar ./throttle-gate.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/throttle-gate.jar"]

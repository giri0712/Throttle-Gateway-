# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage — pinned to specific version for reproducibility & CVE coverage
FROM eclipse-temurin:17-jre_17.0.11-9
WORKDIR /app
COPY --from=build /app/target/throttle-gate-1.0.0-SNAPSHOT.jar ./throttle-gate.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/throttle-gate.jar"]

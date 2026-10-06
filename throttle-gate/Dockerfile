# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage — 17-jre is the current LTS runtime line (matches local JDK 17 and the AWS plan)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/throttle-gate-1.0.0-SNAPSHOT.jar ./throttle-gate.jar
EXPOSE 8080
# Healthcheck targets the public liveness endpoint (see SecurityConfig):
# compose/orchestrators can rely on it without credentials.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status"' || exit 1
ENTRYPOINT ["java","-jar","/app/throttle-gate.jar"]

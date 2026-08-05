FROM maven:3.9.15-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/college-admin-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=30 --start-period=60s \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

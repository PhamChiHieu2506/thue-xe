FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src/main ./src/main
RUN mvn -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/carmanage-service-0.0.1-SNAPSHOT.jar ./app.jar
RUN mkdir -p /app/media && chown -R 1000:1000 /app/media && chmod 1777 /app/media
USER 1000:1000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM gradle:9.5.1-jdk26-noble AS build
WORKDIR /app
COPY . .
RUN ./gradlew :service-registry:bootJar --no-daemon

FROM eclipse-temurin:26-jre-noble
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/service-registry/build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

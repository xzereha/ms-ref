FROM gradle:9.5.1-jdk26-noble AS build
WORKDIR /app
COPY . .
RUN ./gradlew :api-gateway:bootJar --no-daemon

FROM eclipse-temurin:26-jre-noble
WORKDIR /app
COPY --from=build /app/api-gateway/build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

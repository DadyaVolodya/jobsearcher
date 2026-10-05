FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/app.jar /app/app.jar
RUN mkdir -p /data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

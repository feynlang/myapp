FROM eclipse-temurin:17-jdk-noble AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon
RUN cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /tmp/app.jar

FROM eclipse-temurin:17-jre-noble

WORKDIR /app

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring && useradd -r -g spring spring

COPY --from=build /tmp/app.jar /app/app.jar
RUN chown spring:spring /app/app.jar
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
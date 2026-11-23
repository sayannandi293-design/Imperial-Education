FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY build/maven-settings.xml ./maven-settings.xml
COPY src ./src
# Use Google's Maven Central mirror to avoid HTTP 429 on Render shared egress IPs.
# Keep downloaded artifacts in a BuildKit cache across builds where supported.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -s maven-settings.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

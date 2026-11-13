# Stage 1: build with the project's own Maven wrapper (Maven 3.10.0,
# Java 21 — the same wrapper Session 1 proved healthy). Using the
# wrapper instead of a maven:* image means no image-tag guessing:
# the build tool is the one already in the repo.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml ./
COPY src ./src
RUN ./mvnw -B -DskipTests package

# Stage 2: run on a slim JRE only — no JDK, no Maven, no source.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

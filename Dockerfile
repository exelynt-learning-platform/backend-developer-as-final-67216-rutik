# Multi-stage build: compile with Maven, run with a slim JRE.
# Uses the official Maven image (with JDK 17) for the build stage so no
# wrapper script/jar needs to be vendored in the repo.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/resource-booking-system.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

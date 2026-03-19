FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml ./
COPY shared-library/ ./shared-library/
COPY notification-service/ ./notification-service/

RUN mvn clean package -pl notification-service -am -P notification-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/notification-service/target/notification-service-1.0.0.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

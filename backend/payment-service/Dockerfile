FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml ./
COPY shared-library/ ./shared-library/
COPY payment-service/ ./payment-service/

RUN mvn clean package -pl payment-service -am -P payment-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/payment-service/target/payment-service-1.0.0.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

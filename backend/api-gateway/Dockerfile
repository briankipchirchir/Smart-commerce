FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml ./
COPY shared-library/ ./shared-library/
COPY api-gateway/ ./api-gateway/

RUN mvn clean package -pl api-gateway -am -P api-gateway -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/api-gateway/target/api-gateway-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

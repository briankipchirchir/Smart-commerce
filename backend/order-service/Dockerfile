FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml ./
COPY shared-library/ ./shared-library/
COPY order-service/ ./order-service/

# Build everything properly
RUN mvn clean package -pl order-service -am -P order-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/order-service/target/order-service-1.0.0.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
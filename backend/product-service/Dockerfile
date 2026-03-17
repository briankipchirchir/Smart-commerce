FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml ./
COPY shared-library/ ./shared-library/
COPY product-service/ ./product-service/
RUN mvn install -N -DskipTests
RUN mvn install -pl shared-library -DskipTests
RUN mvn package -pl product-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/product-service/target/product-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

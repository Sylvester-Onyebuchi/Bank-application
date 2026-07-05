FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S bank  &&  adduser -S bank -G  bank

COPY --from=build --chown=bank:bank /app/target/*.jar  app.jar

USER bank:bank

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

LABEL authors="sylvester onah"

EXPOSE 8070

ENTRYPOINT ["sh", "-c", " exec java ${JAVA_OPTS} -jar app.jar"]
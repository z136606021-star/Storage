FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY backend/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/storage-backend-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

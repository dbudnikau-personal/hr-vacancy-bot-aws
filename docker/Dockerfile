FROM maven:3.9.15-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

FROM gcr.io/distroless/java21-debian12:nonroot@sha256:e2225eb9aed55a1c5363c34112fe875cb5a4006ef2c7a96051f6cf94497e3a48
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx384m", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]

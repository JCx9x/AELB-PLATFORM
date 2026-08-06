# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q
COPY src src
RUN ./mvnw package -DskipTests -q

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S aelb && adduser -S aelb -G aelb
COPY --from=builder /app/target/*.jar app.jar
USER aelb
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

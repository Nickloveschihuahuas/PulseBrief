# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -q
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the fat jar
COPY --from=build /app/target/*.jar app.jar

# Copy frontend to serve from Spring Boot static resources
COPY frontend/ /app/static/

EXPOSE 8080

# API keys must be provided at runtime via environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]

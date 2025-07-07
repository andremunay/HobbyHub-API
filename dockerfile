# --- Stage 1: Build with Maven ---
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw -B package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy jar from build stage
COPY --from=builder /app/target/*.jar app.jar

# Spring profile (defaults to 'fly' if not set explicitly)
ENV SPRING_PROFILES_ACTIVE=fly

# Optional JVM heap flags – set these only if a special env var is defined
ENV JAVA_OPTS=""

# Fly sets this in fly.toml: `JAVA_OPTS="-Xmx300m -Xms64m"`

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

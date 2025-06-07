# 1) Build stage: compile & package with JDK
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
# Copy everything (including mvnw & .mvn)
COPY . .
# Build the jar, skipping tests (to speed up demo builds)
RUN ./mvnw -B package -DskipTests

# 2) Run stage: slim runtime image with just the JRE
FROM eclipse-temurin:21-jre
# Allow overriding the JAR via build-arg if desired
ARG JAR_FILE=target/*-SNAPSHOT.jar
# Copy the built jar from the builder stage
COPY --from=builder /app/${JAR_FILE} app.jar
# Default entrypoint
ENTRYPOINT ["java","-jar","/app.jar"]

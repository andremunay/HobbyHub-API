# 1) Build stage: compile & package with JDK
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw -B package -DskipTests

# 2) Run stage: slim runtime image with just the JRE
FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/*-SNAPSHOT.jar
COPY --from=builder /app/${JAR_FILE} app.jar

# Set heap size for safe operation in 512MB Fly VM
ENTRYPOINT ["java", "-Xmx300m", "-Xms64m", "-jar", "/app.jar"]

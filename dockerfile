# Use a JDK image
FROM openjdk:21-jdk-slim

# Add the JAR file (you'll need to build it first)
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Run the app
ENTRYPOINT ["java","-jar","/app.jar"]

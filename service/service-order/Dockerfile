FROM openjdk:17-jdk-slim
LABEL authors="wtq"
WORKDIR /app
COPY target/service-order.jar app.jar
EXPOSE 8505
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "app.jar"]
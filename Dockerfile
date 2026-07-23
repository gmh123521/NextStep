FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY nextstep-api/target/nextstep-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

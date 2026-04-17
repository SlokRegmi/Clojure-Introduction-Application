FROM clojure:lein AS builder

WORKDIR /app
COPY project.clj .
RUN lein deps

COPY src src
RUN lein uberjar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/uberjar/test-app-0.1.0-SNAPSHOT-standalone.jar app.jar

EXPOSE 3000
CMD ["java", "-jar", "app.jar"]

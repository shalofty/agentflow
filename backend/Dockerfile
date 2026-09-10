# Multi-stage image for Render Free (512 MB). Do not add disks or paid build features.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y --no-install-recommends maven \
  && mvn -q -DskipTests package \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/agentflow-api-*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"
# Render sets PORT; Spring Boot binds SERVER_PORT / server.port.
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar --server.address=0.0.0.0 --server.port=${PORT:-8080}"]

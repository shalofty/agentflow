# Render Free web service builds from repo root (MCP default dockerContext).
# Keep in sync with backend/Dockerfile intent: Java 21, $PORT, 512 MB-friendly JVM flags.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY backend/pom.xml .
COPY backend/src ./src
RUN apt-get update && apt-get install -y --no-install-recommends maven \
  && mvn -q -DskipTests package \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/agentflow-api-*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar --server.address=0.0.0.0 --server.port=${PORT:-8080}"]

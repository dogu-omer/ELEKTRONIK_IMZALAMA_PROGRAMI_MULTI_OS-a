# syntax=docker/dockerfile:1
# esign REST API kapsayıcısı / container image
# Derleme: docker build -t esign-api .     Çalıştırma: docker compose up -d
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# Testler CI'da koşar; görüntü derlemesinde atlanır. Tests run in CI.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q -pl esign-api -am package -DskipTests

FROM eclipse-temurin:21-jre-noble
LABEL org.opencontainers.image.title="esign-api" \
      org.opencontainers.image.description="CAdES/PAdES/XAdES doğrulama ve iki aşamalı imza REST API" \
      org.opencontainers.image.licenses="GPL-3.0-or-later" \
      org.opencontainers.image.source="https://github.com/dogu-omer/ELEKTRONIK_IMZALAMA_PROGRAMI_MULTI_OS-a"
RUN groupadd --system esign && useradd --system --gid esign --home /app --shell /usr/sbin/nologin esign \
    && mkdir -p /app /trust && chown esign:esign /app
WORKDIR /app
COPY --from=build /src/esign-api/target/esign-api.jar /app/esign-api.jar
USER esign
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/esign-api.jar"]

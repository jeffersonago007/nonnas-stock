# syntax=docker/dockerfile:1.7
# ----- Stage 1: build do reator Maven -----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copia o reator inteiro (todos os módulos).
COPY pom.xml ./
COPY shared-kernel/pom.xml shared-kernel/
COPY web-commons/pom.xml web-commons/
COPY identity/pom.xml identity/
COPY catalog/pom.xml catalog/
COPY inventory-core/pom.xml inventory-core/
COPY recipes/pom.xml recipes/
COPY operations/pom.xml operations/
COPY alerts/pom.xml alerts/
COPY reporting/pom.xml reporting/
COPY sales-channels-api/pom.xml sales-channels-api/
COPY nfe-importer/pom.xml nfe-importer/
COPY quality-tests/pom.xml quality-tests/
COPY app/pom.xml app/

# Pré-baixa dependências (cache de layer Docker melhora rebuilds).
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline -DskipTests || true

# Copia o source completo e compila.
COPY shared-kernel/src shared-kernel/src
COPY web-commons/src web-commons/src
COPY identity/src identity/src
COPY catalog/src catalog/src
COPY inventory-core/src inventory-core/src
COPY recipes/src recipes/src
COPY operations/src operations/src
COPY alerts/src alerts/src
COPY reporting/src reporting/src
COPY app/src app/src
COPY .mvn .mvn
COPY mvnw mvnw
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl app -am package -DskipTests

# ----- Stage 2: runtime mínimo -----
FROM eclipse-temurin:21-jre-alpine

# Curl para healthcheck via wget está embutido; alpine traz wget por default.
RUN addgroup -S nonnas && adduser -S nonnas -G nonnas

WORKDIR /app
COPY --from=build --chown=nonnas:nonnas /workspace/app/target/app-*.jar /app/app.jar

USER nonnas
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O - http://localhost:8080/actuator/health/liveness | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

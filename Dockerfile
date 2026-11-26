# ---- Build ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -Dmaven.test.skip=true dependency:go-offline

COPY src ./src
RUN mvn -B -Dmaven.test.skip=true clean package

# ---- Runtime ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Carpetas de trabajo. Con un disco persistente se monta en /app/uploads;
# en el plan free el filesystem es efímero y se pierde al reiniciar.
RUN mkdir -p /app/uploads/avatars /app/uploads/covers /app/uploads/materials /app/logs

COPY --from=build /workspace/target/api-0.0.1-SNAPSHOT.jar /app/api.jar

# Render entrega DATABASE_URL como postgresql://... y Hikari exige jdbc:postgresql://...
# Este entrypoint normaliza la URL y respeta el puerto que Render inyecta en PORT.
RUN printf '%s\n' \
    '#!/bin/sh' \
    'set -eu' \
    'if [ -n "${DATABASE_URL:-}" ]; then' \
    '  case "$DATABASE_URL" in' \
    '    jdbc:*) ;;' \
    '    postgres://*|postgresql://*) export DATABASE_URL="jdbc:$DATABASE_URL" ;;' \
    '  esac' \
    'fi' \
    'exec java ${JAVA_OPTS:-} -jar /app/api.jar' \
    > /usr/local/bin/entrypoint.sh \
    && chmod 755 /usr/local/bin/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

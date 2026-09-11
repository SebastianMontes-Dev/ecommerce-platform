# syntax=docker/dockerfile:1

# ---------- build ----------
# Compila dentro de la imagen: build reproducible, sin depender de un jar armado en el host.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Capa de dependencias cacheable: solo se invalida si cambian los scripts de Gradle.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test \
    && mv build/libs/*.jar app.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuario sin privilegios: un RCE dentro del contenedor no corre como root.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build --chown=app:app /build/app.jar ./app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

# docker/compose usa esto para el estado del contenedor; k8s usa /health/liveness y /readiness.
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health/readiness | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

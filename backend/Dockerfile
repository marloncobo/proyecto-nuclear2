# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS compilacion

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
    && cp target/psicoapp-*.jar /workspace/aplicacion.jar

FROM eclipse-temurin:21-jre-alpine AS ejecucion

RUN addgroup -S psicoapp && adduser -S -G psicoapp -u 10001 psicoapp

WORKDIR /app

COPY --from=compilacion --chown=psicoapp:psicoapp /workspace/aplicacion.jar ./aplicacion.jar

USER psicoapp
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD nc -z 127.0.0.1 8080 || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/aplicacion.jar"]

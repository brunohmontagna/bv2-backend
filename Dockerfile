# Imagem de producao da API BV2. Build multi-stage: o JDK e o Maven ficam so no
# estagio de build; a imagem final leva apenas o JRE e o jar.

# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Dependencias primeiro: enquanto o pom.xml nao mudar, esta camada vem do cache.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src src
# Os testes rodam no job "test" do GitHub Actions, contra um Postgres real.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
 && cp target/*.jar /src/app.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app \
 && apk add --no-cache tzdata

ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

COPY --from=build --chown=app:app /src/app.jar /app/app.jar
USER app

EXPOSE 8080

# readiness nao depende do banco: um Postgres fora do ar nao derruba as replicas em loop.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

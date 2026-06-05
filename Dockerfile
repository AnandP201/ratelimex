FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -DskipTests package


FROM eclipse-temurin:26-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system ratelimex && useradd --system --gid ratelimex ratelimex

COPY --from=build /workspace/target/*.jar app.jar

RUN chown ratelimex:ratelimex app.jar

USER ratelimex

ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update
ENV SPRING_PROFILES_ACTIVE=prod
ENV RATELIMEX_NAMESPACE=master
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health/readiness" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

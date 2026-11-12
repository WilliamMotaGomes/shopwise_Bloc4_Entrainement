# ---- Etape 1 : build + tests (le build echoue si un test echoue) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package

# ---- Etape 2 : image d'execution minimale (JRE, utilisateur non-root) ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 shopwise
WORKDIR /app
COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar app.jar
USER shopwise
EXPOSE 8080
# Secrets a fournir au runtime : SHOPWISE_JWT_SECRET (>= 32 caracteres) et mots de passe des comptes.
ENTRYPOINT ["java", "-jar", "app.jar"]

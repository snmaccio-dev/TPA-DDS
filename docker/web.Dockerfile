# Imagen de web. El contexto de build es la raiz del repositorio:
#   docker build -f docker/web.Dockerfile -t donatrack-web .

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

COPY pom.xml .
COPY donaciones/pom.xml donaciones/
COPY logistica/pom.xml logistica/
COPY web/pom.xml web/
# Solo el src del modulo que se construye: Maven necesita los pom de los otros para
# armar el reactor, pero no sus fuentes. Ademas mejora el cache de capas.
COPY web/src web/src

RUN mvn -B -q -pl web -am package -DskipTests

# Sin plugin de shade: el jar queda flaco y las dependencias van aparte.
RUN mvn -B -q -pl web dependency:copy-dependencies -DoutputDirectory=/libs \
    -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /libs /app/libs
COPY --from=build /src/web/target/*.jar /app/app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/libs/*", "donatrack.web.WebApp"]

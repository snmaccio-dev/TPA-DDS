# Imagen de logistica. El contexto de build es la raiz del repositorio:
#   docker build -f docker/logistica.Dockerfile -t donatrack-logistica .
#
# La conexion a la base NO entra en el build: se compila con marcadores y se resuelve al
# arrancar, con DB_URL, DB_USER y DB_PASSWORD. La misma imagen sirve en cualquier entorno.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

COPY pom.xml .
COPY donaciones/pom.xml donaciones/
COPY logistica/pom.xml logistica/
COPY web/pom.xml web/
COPY logistica/src logistica/src

RUN mvn -B -q -pl logistica -am package -DskipTests     -Ddb.url=__DB_URL__ -Ddb.user=__DB_USER__ -Ddb.password=__DB_PASSWORD__

RUN mvn -B -q -pl logistica dependency:copy-dependencies -DoutputDirectory=/libs     -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /libs /app/libs
COPY --from=build /src/logistica/target/*.jar /app/app.jar
COPY --from=build /src/logistica/target/classes/META-INF/persistence.xml      /app/config/META-INF/persistence.xml.plantilla
COPY docker/entrypoint-db.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENV MAIN_CLASS=donatrack.logistica.LogisticaApp
EXPOSE 8081
ENTRYPOINT ["/app/entrypoint.sh"]

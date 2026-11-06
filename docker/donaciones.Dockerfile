# Imagen de donaciones. El contexto de build es la raiz del repositorio:
#   docker build -f docker/donaciones.Dockerfile -t donatrack-donaciones .

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# La conexion JDBC se resuelve por filtrado de recursos al compilar, asi que entra
# como build arg. Una imagen construida con estos valores queda atada a ese entorno.
ARG DB_URL
ARG DB_USER
ARG DB_PASSWORD

COPY pom.xml .
COPY donaciones/pom.xml donaciones/
COPY logistica/pom.xml logistica/
COPY web/pom.xml web/
# Solo el src del modulo que se construye: Maven necesita los pom de los otros para
# armar el reactor, pero no sus fuentes. Ademas mejora el cache de capas.
COPY donaciones/src donaciones/src

RUN mvn -B -q -pl donaciones -am package -DskipTests \
    -Ddb.url="$DB_URL" -Ddb.user="$DB_USER" -Ddb.password="$DB_PASSWORD"

# Sin plugin de shade: el jar queda flaco y las dependencias van aparte.
RUN mvn -B -q -pl donaciones dependency:copy-dependencies -DoutputDirectory=/libs \
    -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /libs /app/libs
COPY --from=build /src/donaciones/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/libs/*", "donatrack.donaciones.DonacionesApp"]
